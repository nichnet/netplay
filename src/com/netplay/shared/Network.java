package com.netplay.shared;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main API surface for the netplay library.
 * Provides static methods for handler registration and message sending.
 */
public final class Network {
    private static Serializer serializer;
    private static Transport transport;
    private static final Map<String, HandlerEntry<?>> handlers = new ConcurrentHashMap<>();

    private Network() {
        // Static-only class
    }

    /**
     * Set the serializer to use for message serialization/deserialization.
     * @param serializer the serializer to use
     */
    public static void setSerializer(Serializer serializer) {
        Network.serializer = serializer;
    }

    /**
     * Get the configured serializer.
     * @return the serializer
     */
    public static Serializer getSerializer() {
        return serializer;
    }

    /**
     * Set the transport to use for sending messages.
     * @param transport the transport to use
     */
    public static void setTransport(Transport transport) {
        Network.transport = transport;
    }

    /**
     * Get the configured transport.
     * @return the transport
     */
    public static Transport getTransport() {
        return transport;
    }

    /**
     * Register a handler for a message type.
     * @param messageClass the message class to handle
     * @param handler the handler to invoke when this message type is received
     * @param <T> the message type
     */
    public static <T> void on(Class<T> messageClass, MessageHandler<T> handler) {
        String className = messageClass.getSimpleName();
        handlers.put(className, new HandlerEntry<>(messageClass, handler));
    }

    /**
     * Send a message to a specific connection.
     * @param connectionId the connection to send to
     * @param message the message object to send
     */
    public static void send(String connectionId, Object message) {
        if (transport == null) {
            throw new IllegalStateException("Transport not set. Call Network.setTransport() first.");
        }
        byte[] wireData = toWireFormat(message);
        transport.send(connectionId, wireData);
    }

    /**
     * Broadcast a message to all connections.
     * @param message the message object to broadcast
     */
    public static void broadcast(Object message) {
        if (transport == null) {
            throw new IllegalStateException("Transport not set. Call Network.setTransport() first.");
        }
        byte[] wireData = toWireFormat(message);
        transport.broadcast(wireData);
    }

    /**
     * Broadcast a message to all connections except the specified ones.
     * @param excludeIds connection IDs to exclude
     * @param message the message object to broadcast
     */
    public static void broadcastExcept(String[] excludeIds, Object message) {
        if (transport == null) {
            throw new IllegalStateException("Transport not set. Call Network.setTransport() first.");
        }
        byte[] wireData = toWireFormat(message);
        transport.broadcastExcept(excludeIds, wireData);
    }

    /**
     * Convert a message object to wire format.
     * Wire format: ClassName\n{"json":"body"}\n
     * @param message the message object
     * @return the wire format bytes
     */
    public static byte[] toWireFormat(Object message) {
        if (serializer == null) {
            throw new IllegalStateException("Serializer not set. Call Network.setSerializer() first.");
        }
        String className = message.getClass().getSimpleName();
        byte[] jsonBytes = serializer.serialize(message);
        String wireString = className + "\n" + new String(jsonBytes, StandardCharsets.UTF_8) + "\n";
        return wireString.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Dispatch a received message to the appropriate handler.
     * Called by Client/Server when a complete message is received.
     * @param className the class name from the wire format
     * @param jsonBytes the JSON body bytes
     * @param senderId the connection ID of the sender
     */
    public static void dispatch(String className, byte[] jsonBytes, String senderId) {
        HandlerEntry<?> entry = handlers.get(className);
        if (entry == null) {
            System.err.println("No handler registered for message type: " + className);
            return;
        }
        entry.invoke(jsonBytes, senderId);
    }

    /**
     * Check if a handler is registered for a message type.
     * @param className the simple class name
     * @return true if a handler is registered
     */
    public static boolean hasHandler(String className) {
        return handlers.containsKey(className);
    }

    /**
     * Clear all registered handlers.
     * Useful for testing.
     */
    public static void clearHandlers() {
        handlers.clear();
    }

    /**
     * Internal class to hold handler registration info.
     */
    private static class HandlerEntry<T> {
        private final Class<T> messageClass;
        private final MessageHandler<T> handler;

        HandlerEntry(Class<T> messageClass, MessageHandler<T> handler) {
            this.messageClass = messageClass;
            this.handler = handler;
        }

        void invoke(byte[] jsonBytes, String senderId) {
            if (serializer == null) {
                throw new IllegalStateException("Serializer not set. Call Network.setSerializer() first.");
            }
            T message = serializer.deserialize(jsonBytes, messageClass);
            handler.handle(message, senderId);
        }
    }
}
