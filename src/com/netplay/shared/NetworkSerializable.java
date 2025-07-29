package com.netplay.shared;

/**
 * Interface for objects that can be serialized for network transmission.
 * Classes implementing this interface can be serialized/deserialized regardless
 * of their inheritance hierarchy, making it more flexible than extending an abstract class.
 */
public interface NetworkSerializable {
    /**
     * Serialize this object to a byte array using NetworkSerializer.
     * @return byte array representation of this object
     */
    default byte[] serialize() {
        return NetworkSerializer.serialize(this);
    }
    
    /**
     * Populate this object's fields from serialized byte data using NetworkSerializer.
     * @param data the byte array to deserialize from
     * @throws Exception if deserialization fails
     */
    default void readFrom(byte[] data) throws Exception {
        NetworkSerializer.deserialize(this, data);
    }
}
