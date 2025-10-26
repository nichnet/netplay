package com.netplay.client;

import com.netplay.shared.MessageChunker;
import com.netplay.shared.NetworkConstants;
import com.netplay.shared.events.NetworkEventBus;
import com.netplay.shared.messages.NetworkMessage;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public abstract class Client {
  private static Client instance;

  private String host;
  private int port;
  private boolean connected;

  private SocketChannel socketChannel;
  private Selector selector;
  private Thread readerThread;
  private ByteBuffer readBuffer;

  private NetworkEventBus eventBus;

  // For reassembling chunked messages
  private byte[] messageReassemblyBuffer;
  private int messageReassemblyOffset;

  public Client() {
    instance = this;
    this.readBuffer = ByteBuffer.allocate(NetworkConstants.BUFFER_SIZE);
    this.eventBus = new NetworkEventBus();
    this.messageReassemblyBuffer = null;
    this.messageReassemblyOffset = 0;
  }


  public final void registerHandlerPackages(String... packageNames) {
    eventBus.registerHandlers(packageNames);
  }

  public final void connect(String host, int port) {
    if (host == null) {
      System.out.print("Could not connect. Missing host.");
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
        // Chunk the message if needed
        byte[] messageBytes = networkMessage.toBytes();
        List<byte[]> chunks = MessageChunker.chunkMessage(messageBytes);

        // Send each chunk
        for (byte[] chunk : chunks) {
          socketChannel.write(ByteBuffer.wrap(chunk));
        }
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

      // Process all complete chunks in the buffer
      while (readBuffer.remaining() >= 1) {
        // Read chunk header to determine chunk size
        byte header = readBuffer.get(readBuffer.position());

        // Read the entire remaining buffer as a chunk
        int chunkSize = readBuffer.remaining();
        if (chunkSize > NetworkConstants.BUFFER_SIZE) {
          chunkSize = NetworkConstants.BUFFER_SIZE;
        }

        // Extract chunk
        byte[] chunkBytes = new byte[chunkSize];
        readBuffer.get(chunkBytes);

        // Process chunk and check if it completes a message
        byte[] completeMessage = processChunk(chunkBytes);

        if (completeMessage != null) {
          // Message is complete, parse and handle it
          try {
            NetworkMessage networkMessage = NetworkMessage.fromBytes(completeMessage);
            // It's irrelevant to set the message sender id because
            // messages can only come from the server, but just for
            // the sake of it mark it as from the server.
            networkMessage.setSenderConnectionId("SERVER");

            eventBus.handleReceivedMessage(networkMessage);
          } catch (IOException e) {
            System.err.println("Error parsing message from server: " + e.getMessage());
          }
        }

        // If we consumed all data, break out
        if (!readBuffer.hasRemaining()) {
          break;
        }
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

  /**
   * Process a chunk and reassemble if needed.
   * @param chunkBytes the chunk bytes (including header)
   * @return the complete message if this was the final chunk, null otherwise
   */
  private byte[] processChunk(byte[] chunkBytes) {
    if (chunkBytes.length == 0) {
      return null;
    }

    byte header = chunkBytes[0];
    byte[] chunkData = MessageChunker.extractChunkData(chunkBytes);

    boolean isContinuation = MessageChunker.isContinuation(header);
    boolean hasMore = MessageChunker.hasMore(header);

    // First chunk of a message
    if (!isContinuation) {
      // Start new reassembly
      if (hasMore) {
        // Multi-chunk message starting
        messageReassemblyBuffer = new byte[NetworkConstants.MAX_MESSAGE_SIZE];
        messageReassemblyOffset = 0;
        System.arraycopy(chunkData, 0, messageReassemblyBuffer, messageReassemblyOffset, chunkData.length);
        messageReassemblyOffset += chunkData.length;
        return null; // Not complete yet
      } else {
        // Single-chunk message
        return chunkData;
      }
    } else {
      // Continuation chunk
      if (messageReassemblyBuffer == null) {
        System.err.println("Received continuation chunk without starting chunk - discarding");
        return null;
      }

      // Append to reassembly buffer
      System.arraycopy(chunkData, 0, messageReassemblyBuffer, messageReassemblyOffset, chunkData.length);
      messageReassemblyOffset += chunkData.length;

      if (!hasMore) {
        // Final chunk - extract complete message
        byte[] completeMessage = new byte[messageReassemblyOffset];
        System.arraycopy(messageReassemblyBuffer, 0, completeMessage, 0, messageReassemblyOffset);

        // Reset reassembly state
        messageReassemblyBuffer = null;
        messageReassemblyOffset = 0;

        return completeMessage;
      }

      return null; // More chunks expected
    }
  }
}
