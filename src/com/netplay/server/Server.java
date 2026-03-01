package com.netplay.server;

import com.netplay.shared.Network;
import com.netplay.shared.Transport;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TCP server for netplay.
 * Implements Transport to allow Network to send messages.
 */
public abstract class Server implements Transport {
    private String host;
    private int port;
    private int maxConnections;
    private Selector selector;
    private ServerSocketChannel serverSocketChannel;
    private boolean running;

    private final ConcurrentHashMap<String, NetworkConnection> connections = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<SocketChannel, NetworkConnection> channelToConnection = new ConcurrentHashMap<>();

    public Server() {
    }

    /**
     * Start the server.
     * @param host the host to bind to
     * @param port the port to bind to
     */
    public final void start(String host, int port) {
        if (isRunning()) {
            System.err.println("Server already started");
            return;
        }
        this.host = host;
        this.port = port;

        // Register this server as the transport
        Network.setTransport(this);

        new Thread(() -> {
            try {
                selector = Selector.open();
                serverSocketChannel = ServerSocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.bind(new InetSocketAddress(host, port));
                serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

                running = true;
                System.out.println("Server started: " + getAddress());
            } catch (IOException e) {
                System.err.println("Failed to start server: " + e.getMessage());
                return;
            }

            // Single thread event loop
            try {
                while (running) {
                    selector.select();

                    Set<SelectionKey> selectedKeys = selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();

                    while (iterator.hasNext()) {
                        SelectionKey key = iterator.next();
                        iterator.remove();

                        try {
                            if (key.isAcceptable()) {
                                acceptConnection();
                            } else if (key.isReadable()) {
                                readFromClient(key);
                            } else if (key.isWritable()) {
                                writeToClient(key);
                            }
                        } catch (IOException e) {
                            if (running) {
                                SocketChannel clientChannel = (SocketChannel) key.channel();
                                NetworkConnection connection = channelToConnection.get(clientChannel);
                                if (connection != null) {
                                    disconnectUser(connection);
                                }
                                key.cancel();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (running) {
                    System.err.println("Server error: " + e.getMessage());
                }
            }
        }).start();
    }

    private void acceptConnection() throws IOException {
        SocketChannel clientChannel = serverSocketChannel.accept();
        if (clientChannel == null) {
            return;
        }
        if (maxConnections > 0 && connections.size() >= maxConnections) {
            System.out.println("Max connections reached, rejecting client");
            clientChannel.close();
            return;
        }

        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ);

        String id = generateId();
        NetworkConnection connection = new NetworkConnection(id, clientChannel);
        connections.put(id, connection);
        channelToConnection.put(clientChannel, connection);

        onUserConnected(connection);
    }

    private void readFromClient(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        NetworkConnection connection = channelToConnection.get(clientChannel);

        if (connection == null) {
            return;
        }

        ByteBuffer buffer = connection.getReadBuffer();
        buffer.clear();

        int bytesRead = clientChannel.read(buffer);

        if (bytesRead == -1) {
            disconnectUser(connection);
            return;
        }

        if (bytesRead > 0) {
            buffer.flip();
            connection.appendToMessageBuffer(buffer);
            connection.processMessages();
        }
    }

    private void writeToClient(SelectionKey key) throws IOException {
        SocketChannel clientChannel = (SocketChannel) key.channel();
        NetworkConnection connection = channelToConnection.get(clientChannel);

        if (connection == null) {
            return;
        }

        try {
            boolean hasMoreWrites = connection.processWrites();
            if (!hasMoreWrites) {
                key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            disconnectUser(connection);
        }
    }

    private void disconnectUser(NetworkConnection userConnection) {
        connections.remove(userConnection.getId());
        channelToConnection.remove(userConnection.getChannel());

        userConnection.disconnect();

        onUserDisconnected(userConnection);
    }

    /**
     * Stop the server.
     */
    public final void stop() {
        running = false;

        if (selector != null) {
            selector.wakeup();
        }

        for (NetworkConnection connection : connections.values()) {
            disconnectUser(connection);
        }
        connections.clear();
        channelToConnection.clear();

        try {
            if (serverSocketChannel != null) {
                serverSocketChannel.close();
            }
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }

        System.out.println("Server stopped");
    }

    /**
     * Kick a connection.
     * @param connectionId the connection to kick
     */
    public final void kick(String connectionId) {
        NetworkConnection connection = connections.get(connectionId);
        if (connection == null) {
            return;
        }

        disconnectUser(connection);
    }

    // Transport interface implementation

    @Override
    public void send(String connectionId, byte[] data) {
        NetworkConnection connection = connections.get(connectionId);
        if (connection == null || !connection.isConnected()) {
            return;
        }
        queueWrite(connection, data);
    }

    @Override
    public void broadcast(byte[] data) {
        connections.values().parallelStream()
            .forEach(connection -> queueWrite(connection, data));
    }

    @Override
    public void broadcastExcept(String[] excludeIds, byte[] data) {
        Set<String> excludeSet = Set.of(excludeIds);
        connections.entrySet().parallelStream()
            .filter(entry -> !excludeSet.contains(entry.getKey()))
            .map(Map.Entry::getValue)
            .forEach(connection -> queueWrite(connection, data));
    }

    @Override
    public void disconnect(String connectionId) {
        kick(connectionId);
    }

    @Override
    public boolean isConnected(String connectionId) {
        NetworkConnection connection = connections.get(connectionId);
        return connection != null && connection.isConnected();
    }

    private void queueWrite(NetworkConnection connection, byte[] data) {
        if (!isRunning() || !connection.isConnected()) {
            return;
        }

        try {
            connection.queueMessage(data);
            SelectionKey key = connection.getChannel().keyFor(selector);
            if (key != null && key.isValid()) {
                key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
                selector.wakeup();
            }
        } catch (Exception e) {
            System.err.println("Error queueing message to " + connection.getId() + ": " + e.getMessage());
        }
    }

    /**
     * Called when a user connects.
     * @param connection the new connection
     */
    public abstract void onUserConnected(NetworkConnection connection);

    /**
     * Called when a user disconnects.
     * @param connection the disconnected connection
     */
    public abstract void onUserDisconnected(NetworkConnection connection);

    public final NetworkConnection getConnection(String id) {
        return connections.get(id);
    }

    public final int getConnectedCount() {
        return connections.size();
    }

    public final boolean isUserConnected(String id) {
        NetworkConnection connection = getConnection(id);
        return connection != null && connection.isConnected();
    }

    public final void setMaxConnections(int maxConnections) {
        if (isRunning()) {
            System.err.println("Cannot set the server max connections whilst it is running.");
            return;
        }
        this.maxConnections = maxConnections;
    }

    public final int getMaxConnections() {
        return maxConnections;
    }

    public final boolean isFull() {
        return maxConnections > 0 && getConnectedCount() >= getMaxConnections();
    }

    public final boolean isRunning() {
        return running;
    }

    public final String getHost() {
        return host;
    }

    public final int getPort() {
        return port;
    }

    public final String getAddress() {
        return getHost() + ":" + getPort();
    }

    public final void setPort(int port) {
        if (isRunning()) {
            System.err.println("Cannot set the server port whilst it is running.");
            return;
        }
        this.port = port;
    }

    public final void setHost(String host) {
        if (isRunning()) {
            System.err.println("Cannot set the server host whilst it is running.");
            return;
        }
        this.host = host;
    }

    private static String generateId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }
}
