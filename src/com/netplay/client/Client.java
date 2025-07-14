package com.netplay.client;

import com.netplay.shared.NetworkConstants;
import com.netplay.shared.actions.NetworkActions;
import com.netplay.shared.messages.NetworkMessage;
import com.netplay.shared.messages.NetworkMessageRegistry;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Set;

public abstract class Client {
  private static Client instance;

  private String host;
  private int port;
  private String username;
  private String password;
  private boolean connected;

  private SocketChannel socketChannel;
  private Selector selector;
  private Thread readerThread;
  private ByteBuffer readBuffer;

  private NetworkActions clientActions;

  public Client() {
    instance = this;
    this.readBuffer = ByteBuffer.allocate(NetworkConstants.BUFFER_SIZE);
  }

  public final void registerMessagePackages(String... packagePaths) throws Exception {
    new NetworkMessageRegistry(packagePaths);
  }

  public final void registerActions(NetworkActions clientActions) {
    this.clientActions = clientActions;
  }

  public final void connect(String host, int port, String username, String password) {
    if (host == null || username == null || password == null) {
      System.out.print("Could not connect. Missing credentials.");
      return;
    }

    this.host = host;
    this.port = port;
    this.username = username;
    this.password = password;

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
   * Send a message to the server
   */
  public final void sendNetworkMessage(NetworkMessage networkMessage) {
    if (connected && socketChannel != null && socketChannel.isConnected()) {
      try {
        socketChannel.write(ByteBuffer.wrap(networkMessage.toBytes()));
      } catch (IOException e) {
        System.err.println("Error sending message: " + e.getMessage());
        connected = false;
      }
    }
  }

  private final void readMessages() {
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
      // Connection closed - this is normal when server shuts down or client disconnects
      connected = false;
      onDisconnected();
    }
  }

  private final void onConnectedToServer() throws IOException {
    if (!socketChannel.finishConnect()) {
      return;
    }

    socketChannel.register(selector, SelectionKey.OP_READ);
    onConnected();
  }

  public abstract void onConnected();
  public abstract void onDisconnected();

  private final void readFromServer() throws IOException {
    readBuffer.compact(); // Preserve any existing partial data
    int bytesRead = socketChannel.read(readBuffer);

    if (bytesRead == -1) {
      connected = false;
      return;
    }

    if (bytesRead > 0) {
      readBuffer.flip();

      // Process all complete messages in the buffer
      while (readBuffer.remaining() >= 2) {
        // Check if we have at least 2 bytes to read the message length
        int messageLength = readBuffer.getShort(readBuffer.position()) & 0xFFFF;
        int totalMessageSize = messageLength + 2; // +2 for the length field itself

        // Check if we have the complete message
        if (readBuffer.remaining() < totalMessageSize) {
          break; // Wait for more data
        }

        // We have a complete message, extract it
        byte[] bytes = new byte[totalMessageSize];
        readBuffer.get(bytes);

        NetworkMessage networkMessage = NetworkMessage.fromBytes(bytes);
        // It's irrelevant to set the message sender id because
        // messages can only come from the server, but just for
        // the sake of it mark it as from the server.
        networkMessage.setSenderConnectionId("SERVER");

        clientActions.handleReceivedMessage(networkMessage);
      }
    }
  }

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

  public String getUsername() {
    return username;
  }

  public String getPassword() {
    return password;
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
