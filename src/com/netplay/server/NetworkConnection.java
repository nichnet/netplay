package com.netplay.server;

import com.netplay.shared.MessageChunker;
import com.netplay.shared.NetworkConstants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkConnection {
  private String id;
  private SocketChannel channel;
  private ByteBuffer readBuffer;
  private ByteBuffer writeBuffer;
  private Queue<ByteBuffer> writeQueue;
  private boolean connected;

  // For reassembling chunked messages
  private byte[] messageReassemblyBuffer;
  private int messageReassemblyOffset;

  public NetworkConnection(String id, SocketChannel channel) {
    this.id = id;
    this.channel = channel;
    this.readBuffer = ByteBuffer.allocate(NetworkConstants.BUFFER_SIZE);
    this.writeBuffer = ByteBuffer.allocate(NetworkConstants.BUFFER_SIZE);
    this.writeQueue = new ConcurrentLinkedQueue<>();
    this.connected = true;
    this.messageReassemblyBuffer = null;
    this.messageReassemblyOffset = 0;
  }

  public String getId() {
    return id;
  }

  public boolean isConnected() {
    return connected && channel.isConnected();
  }

  public void disconnect() {
    connected = false;
    try {
      if (channel != null && channel.isOpen()) {
        channel.close();
      }
    } catch (IOException e) {
      System.err.println("Error closing connection for connection: " + id + ": " + e.getMessage());
    }
  }

  public SocketChannel getChannel() {
    return channel;
  }

  public ByteBuffer getReadBuffer() {
    return readBuffer;
  }

  public void queueMessage(byte[] messageBytes) {
    // Chunk the message if it's too large
    List<byte[]> chunks = MessageChunker.chunkMessage(messageBytes);

    // Queue each chunk separately
    for (byte[] chunk : chunks) {
      writeQueue.offer(ByteBuffer.wrap(chunk));
    }
  }

  public boolean processWrites() throws IOException {
    if (writeBuffer.position() == 0) {
      ByteBuffer nextMessage = writeQueue.poll();
      if (nextMessage == null) {
        return false;
      }
      writeBuffer.put(nextMessage);
      writeBuffer.flip();
    }

    int bytesWritten = channel.write(writeBuffer);
    if (!writeBuffer.hasRemaining()) {
      writeBuffer.clear();
      return !writeQueue.isEmpty();
    }

    return true;
  }

  public String getRemoteAddress() {
    try {
      if (channel != null && channel.isOpen()) {
        return channel.getRemoteAddress().toString();
      } else {
        return "[closed]";
      }
    } catch (IOException e) {
      return "[unknown]";
    }
  }

  /**
   * Process a chunk and reassemble if needed.
   * @param chunkBytes the chunk bytes (including header)
   * @return the complete message if this was the final chunk, null otherwise
   */
  public byte[] processChunk(byte[] chunkBytes) {
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

  @Override
  public String toString() {
    return "Connection{id='" + id + "', address=" + getRemoteAddress() + "}";
  }
}
