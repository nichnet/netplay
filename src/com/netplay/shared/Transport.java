package com.netplay.shared;

/**
 * Interface for network transport operations.
 * The Server implements this to allow Network to send messages.
 */
public interface Transport {
    /**
     * Send data to a specific connection.
     * @param connectionId the connection to send to
     * @param data the data to send
     */
    void send(String connectionId, byte[] data);

    /**
     * Broadcast data to all connections.
     * @param data the data to broadcast
     */
    void broadcast(byte[] data);

    /**
     * Broadcast data to all connections except the specified ones.
     * @param excludeIds connection IDs to exclude
     * @param data the data to broadcast
     */
    void broadcastExcept(String[] excludeIds, byte[] data);

    /**
     * Disconnect a connection.
     * @param connectionId the connection to disconnect
     */
    void disconnect(String connectionId);

    /**
     * Check if a connection is connected.
     * @param connectionId the connection to check
     * @return true if connected
     */
    boolean isConnected(String connectionId);
}
