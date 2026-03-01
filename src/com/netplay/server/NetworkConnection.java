package com.netplay.server;

import com.netplay.shared.Network;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents a single connected client on the server.
 */
public class NetworkConnection {
    private static final int READ_BUFFER_SIZE = 8192;

    private final String id;
    private final SocketChannel channel;
    private final ByteBuffer readBuffer;
    private final Queue<ByteBuffer> writeQueue;
    private ByteBuffer currentWriteBuffer;
    private boolean connected;

    // Buffer for accumulating partial messages
    private final StringBuilder messageBuffer;

    public NetworkConnection(String id, SocketChannel channel) {
        this.id = id;
        this.channel = channel;
        this.readBuffer = ByteBuffer.allocate(READ_BUFFER_SIZE);
        this.writeQueue = new ConcurrentLinkedQueue<>();
        this.currentWriteBuffer = null;
        this.connected = true;
        this.messageBuffer = new StringBuilder();
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

    /**
     * Append data from the read buffer to the message buffer.
     * @param buffer the buffer containing received data
     */
    public void appendToMessageBuffer(ByteBuffer buffer) {
        String received = StandardCharsets.UTF_8.decode(buffer).toString();
        messageBuffer.append(received);
    }

    /**
     * Process complete messages from the message buffer.
     * Wire format: ClassName\nJSON\n
     */
    public void processMessages() {
        String buffer = messageBuffer.toString();

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
                Network.dispatch(className, jsonBytes, id);
            } catch (Exception e) {
                System.err.println("Error handling message from " + id + ": " + e.getMessage());
            }

            // Remove processed message from buffer
            buffer = buffer.substring(secondNewline + 1);
        }

        // Store remaining partial data
        messageBuffer.setLength(0);
        messageBuffer.append(buffer);
    }

    /**
     * Queue a message to be sent.
     * @param data the raw bytes to send
     */
    public void queueMessage(byte[] data) {
        writeQueue.offer(ByteBuffer.wrap(data));
    }

    /**
     * Process queued writes.
     * @return true if there are more writes pending
     */
    public boolean processWrites() throws IOException {
        // Get next message if we don't have one in progress
        if (currentWriteBuffer == null || !currentWriteBuffer.hasRemaining()) {
            currentWriteBuffer = writeQueue.poll();
            if (currentWriteBuffer == null) {
                return false;
            }
        }

        // Write directly from the message buffer (no size limit)
        channel.write(currentWriteBuffer);

        if (!currentWriteBuffer.hasRemaining()) {
            currentWriteBuffer = null;
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
