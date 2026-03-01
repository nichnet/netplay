package com.netplay.shared;

/**
 * Interface for message serialization.
 * Implement this interface to provide custom serialization strategies.
 */
public interface Serializer {
    /**
     * Serialize an object to bytes.
     * @param obj the object to serialize
     * @return the serialized bytes
     */
    byte[] serialize(Object obj);

    /**
     * Deserialize bytes into an object of the specified class.
     * @param data the bytes to deserialize
     * @param clazz the class to deserialize into
     * @param <T> the type of the object
     * @return the deserialized object
     */
    <T> T deserialize(byte[] data, Class<T> clazz);
}
