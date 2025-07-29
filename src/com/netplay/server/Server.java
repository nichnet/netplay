package com.netplay.server;

import com.netplay.shared.NetworkConstants;
import com.netplay.shared.events.NetworkEventBus;
import com.netplay.shared.messages.NetworkMessage;
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
import java.util.concurrent.ConcurrentHashMap;

public abstract class Server {
  private String host;
  private int port;
  private int maxConnections;
  private Selector selector;
  private ServerSocketChannel serverSocketChannel;
  private boolean running;

  private final ConcurrentHashMap<String, NetworkConnection> connections = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<SocketChannel, NetworkConnection> channelToConnection = new ConcurrentHashMap<>();

  private NetworkEventBus eventBus;

  public Server() {
    this.eventBus = new NetworkEventBus();
  }


  public final void registerHandlerPackages(String... packageNames) {
    eventBus.registerHandlers(packageNames);
  }

  public final void start(String host, int port) {
    if (isRunning()) {
      System.err.println("Server already started");
      return;
    }
    this.host = host;
    this.port = port;
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
              // Handle individual connection errors - silently clean up
              if (running) {
                // Clean up the failed connection
                SocketChannel clientChannel = (SocketChannel) key.channel();
                NetworkConnection connection = channelToConnection.get(clientChannel);
                if (connection != null) {
                  disconnectUser(connection);
                }
                key.cancel(); // Remove from selector
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

  private final void acceptConnection() throws IOException {
    SocketChannel clientChannel = serverSocketChannel.accept();
    if (clientChannel == null) {
      return;
    }
    if (connections.size() >= maxConnections) {
      System.out.println("Max connections reached, rejecting client");
      clientChannel.close();
      return;
    }

    clientChannel.configureBlocking(false);
    clientChannel.register(selector, SelectionKey.OP_READ);

    String id = NetworkConstants.generateId();
    NetworkConnection connection = new NetworkConnection(id, clientChannel);
    connections.put(id, connection);
    channelToConnection.put(clientChannel, connection);

    onUserConnected(connection);
  }

  private final void readFromClient(SelectionKey key) throws IOException {
    SocketChannel clientChannel = (SocketChannel) key.channel();
    NetworkConnection connection = channelToConnection.get(clientChannel);

    if (connection == null) {
      return;
    }

    ByteBuffer buffer = connection.getReadBuffer();
    buffer.compact(); // Preserve any existing partial data

    int bytesRead = clientChannel.read(buffer);

    if (bytesRead == -1) {
      disconnectUser(connection);
      return;
    }

    if (bytesRead > 0) {
      buffer.flip();

      // Process all complete messages in the buffer
      while (buffer.remaining() >= 2) {
        // Check if we have at least 2 bytes to read the message length
        int messageLength = buffer.getShort(buffer.position()) & 0xFFFF;
        int totalMessageSize = messageLength + 2; // +2 for the length field itself

        // Check if we have the complete message
        if (buffer.remaining() < totalMessageSize) {
          break; // Wait for more data
        }

        // We have a complete message, extract it
        byte[] bytes = new byte[totalMessageSize];
        buffer.get(bytes);
        NetworkMessage networkMessage = NetworkMessage.fromBytes(bytes);
        networkMessage.setSenderConnectionId(connection.getId());

        eventBus.handleReceivedMessage(networkMessage);
      }
    }
  }

  private final void writeToClient(SelectionKey key) throws IOException {
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

  private final void disconnectUser(NetworkConnection userConnection) {
    connections.remove(userConnection.getId());
    channelToConnection.remove(userConnection.getChannel());

    userConnection.disconnect();

    onUserDisconnected(userConnection);
  }

  public final void stop() {
    running = false;

    // Wake up the selector to break out of select() call
    if (selector != null) {
      selector.wakeup();
    }

    // Disconnect all users
    for (NetworkConnection connection : connections.values()) {
      disconnectUser(connection);
    }
    connections.clear();
    channelToConnection.clear();

    // Close server socket and selector
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

  public final void kick(String connectionId) {
    NetworkConnection connection = connections.get(connectionId);
    if (connection == null) {
      return;
    }

    disconnectUser(connection);
  }

  public final void sendMessageBroadcast(NetworkMessage networkMessage) {
    // Send the messages in parallel to create equality
    connections.values().parallelStream()
      .forEach(connection -> sendNetworkMessage(connection, networkMessage));
  }

  public final void sendMessageBroadcastExcept(String[] excludeConnectionIds, NetworkMessage networkMessage) {
    Set<String> excludeSet = Set.of(excludeConnectionIds);

    // Send the messages in parallel to create equality
    connections.entrySet().parallelStream()
      .filter(entry -> !excludeSet.contains(entry.getKey()))
      .map(Map.Entry::getValue)
      .forEach(connection -> sendNetworkMessage(connection, networkMessage));
  }

  public final void sendMessageMulticast(String[] connectionIds, NetworkMessage networkMessage) {
    // Send the messages in parallel to create equality
    Arrays.stream(connectionIds)
      .parallel()
      .filter(connections::containsKey)  // Filter valid connections
      .forEach(connectionId -> sendNetworkMessage(connections.get(connectionId), networkMessage));
  }

  public final void sendMessageUnicast(String connectionId, NetworkMessage networkMessage) {
    if (connections.containsKey(connectionId)) {
      sendNetworkMessage(connections.get(connectionId), networkMessage);
    }
  }

  private final void sendNetworkMessage(NetworkConnection connection, NetworkMessage networkMessage) {
    if (connection == null) {
      System.err.println("Failed to send message: No connection id provided.");
      return;
    }

    if (!isRunning()) {
      System.err.println("Failed to send message: server not running.");
      return;
    }

    if (!connection.isConnected()) {
      System.err.println("Failed to send message: connection " + connection.getId() + " not connected.");
      return;
    }

    try {
      connection.queueMessage(networkMessage.toBytes());
      SelectionKey key = connection.getChannel().keyFor(selector);
      if (key != null && key.isValid()) {
        key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        selector.wakeup(); // Force selector to re-evaluate
      }
    } catch (Exception e) {
      System.err.println("Error queueing message to " + connection.getId() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }

  public abstract void onUserConnected(NetworkConnection connection);

  public abstract void onUserDisconnected(NetworkConnection connection);

  public final NetworkConnection getConnection(String id) {
    return connections.get(id);
  }

  public final int getConnectedCount() {
    return connections.size();
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
    return getConnectedCount() >= getMaxConnections();
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
}
