package com.netplay.server;

import com.netplay.shared.NetworkConstants;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NetworkConnection {
  private String id;
  private SocketChannel channel;
  private ByteBuffer readBuffer;
  private ByteBuffer writeBuffer;
  private Queue<ByteBuffer> writeQueue;
  private boolean connected;

  public NetworkConnection(String id, SocketChannel channel) {
    this.id = id;
    this.channel = channel;
    this.readBuffer = ByteBuffer.allocate(NetworkConstants.BUFFER_SIZE);
    this.writeBuffer = ByteBuffer.allocate(NetworkConstants.BUFFER_SIZE);
    this.writeQueue = new ConcurrentLinkedQueue<>();
    this.connected = true;
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
    writeQueue.offer(ByteBuffer.wrap(messageBytes));
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

  @Override
  public String toString() {
    return "Connection{id='" + id + "', address=" + getRemoteAddress() + "}";
  }
}
