package com.netplay.shared;

/**
 * Functional interface for handling incoming messages.
 * @param <T> the type of message this handler processes
 */
@FunctionalInterface
public interface MessageHandler<T> {
    /**
     * Handle an incoming message.
     * @param message the deserialized message
     * @param senderId the connection ID of the sender
     */
    void handle(T message, String senderId);
}
