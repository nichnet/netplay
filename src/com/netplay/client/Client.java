package com.netplay.client;

import com.netplay.shared.Network;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Set;

/**
 * TCP client for connecting to a netplay server.
 */
public abstract class Client {
    private static final int BUFFER_SIZE = 8192;

    private static Client instance;

    private String host;
    private int port;
    private boolean connected;

    private SocketChannel socketChannel;
    private Selector selector;
    private Thread readerThread;
    private ByteBuffer readBuffer;

    // Buffer for accumulating partial messages
    private StringBuilder messageBuffer;

    public Client() {
        instance = this;
        this.readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.messageBuffer = new StringBuilder();
    }

    /**
     * Connect to the server.
     * @param host the server host
     * @param port the server port
     */
    public final void connect(String host, int port) {
        if (host == null) {
            System.err.println("Could not connect. Missing host.");
            return;
        }

        this.host = host;
        this.port = port;

        try {
            System.out.println("Connecting to server at " + host + ":" + port);

            selector = Selector.open();
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            socketChannel.connect(new InetSocketAddress(host, port));
            socketChannel.register(selector, SelectionKey.OP_CONNECT);

            connected = true;

            readerThread = new Thread(this::readMessages);
            readerThread.start();
        } catch (IOException e) {
            System.err.println("Failed to connect to server: " + e.getMessage());
            connected = false;
        }
    }

    /**
     * Disconnect from the server.
     */
    public final void disconnect() {
        connected = false;

        if (readerThread != null) {
            readerThread.interrupt();
        }

        try {
            if (socketChannel != null && socketChannel.isOpen()) {
                socketChannel.close();
            }
            if (selector != null) {
                selector.close();
            }
        } catch (IOException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }

        System.out.println("Disconnected from server");
    }

    /**
     * Send a message to the server.
     * @param message the message object to send
     */
    public final void send(Object message) {
        if (connected && socketChannel != null && socketChannel.isConnected()) {
            try {
                byte[] wireData = Network.toWireFormat(message);
                socketChannel.write(ByteBuffer.wrap(wireData));
            } catch (IOException e) {
                System.err.println("Error sending message: " + e.getMessage());
                connected = false;
            }
        }
    }

    private void readMessages() {
        try {
            while (connected) {
                selector.select();

                Set<SelectionKey> selectedKeys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = selectedKeys.iterator();

                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();

                    if (key.isConnectable()) {
                        onConnectedToServer();
                    } else if (key.isReadable()) {
                        readFromServer();
                    }
                }
            }
        } catch (IOException e) {
            connected = false;
            onDisconnected();
        }
    }

    private void onConnectedToServer() {
        try {
            if (!socketChannel.finishConnect()) {
                return;
            }
            socketChannel.register(selector, SelectionKey.OP_READ);
            onConnected();
        } catch (IOException e) {
            connected = false;
            onConnectionFailed();
        }
    }

    /**
     * Called when connection to server is established.
     */
    public abstract void onConnected();

    /**
     * Called when initial connection to server fails (server unreachable).
     */
    public abstract void onConnectionFailed();

    /**
     * Called when disconnected from server after being connected.
     */
    public abstract void onDisconnected();

    private void readFromServer() throws IOException {
        readBuffer.clear();
        int bytesRead = socketChannel.read(readBuffer);

        if (bytesRead == -1) {
            connected = false;
            onDisconnected();
            return;
        }

        if (bytesRead > 0) {
            readBuffer.flip();
            String received = StandardCharsets.UTF_8.decode(readBuffer).toString();
            messageBuffer.append(received);

            // Process complete messages (wire format: ClassName\nJSON\n)
            processMessageBuffer();
        }
    }

    private void processMessageBuffer() {
        String buffer = messageBuffer.toString();

        // Each complete message is: ClassName\nJSON\n
        // So we need at least two newlines for a complete message
        while (true) {
            int firstNewline = buffer.indexOf('\n');
            if (firstNewline == -1) {
                break; // No complete line yet
            }

            int secondNewline = buffer.indexOf('\n', firstNewline + 1);
            if (secondNewline == -1) {
                break; // No complete message yet
            }

            // Extract class name and JSON body
            String className = buffer.substring(0, firstNewline);
            String jsonBody = buffer.substring(firstNewline + 1, secondNewline);

            // Dispatch to Network
            try {
                byte[] jsonBytes = jsonBody.getBytes(StandardCharsets.UTF_8);
                Network.dispatch(className, jsonBytes, "SERVER");
            } catch (Exception e) {
                System.err.println("Error handling message: " + e.getMessage());
            }

            // Remove processed message from buffer
            buffer = buffer.substring(secondNewline + 1);
        }

        // Store remaining partial data
        messageBuffer = new StringBuilder(buffer);
    }

    /**
     * Check if connected to server.
     * @return true if connected
     */
    public final boolean isConnected() {
        return connected && socketChannel != null && socketChannel.isConnected();
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

    public static Client getInstance() {
        return instance;
    }

    public final void setHost(String host) {
        if (isConnected()) {
            System.err.println("Cannot set the server host whilst it is running.");
            return;
        }
        this.host = host;
    }

    public final void setPort(int port) {
        if (isConnected()) {
            System.err.println("Cannot set the server port whilst it is running.");
            return;
        }
        this.port = port;
    }
}
