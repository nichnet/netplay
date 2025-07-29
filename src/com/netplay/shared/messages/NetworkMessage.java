package com.netplay.shared.messages;

import com.netplay.shared.CompressionUtil;
import com.netplay.shared.NetworkSerializable;

import java.io.*;
import java.lang.reflect.Method;

public class NetworkMessage {
  private static final int SIZE_OF_UINT8 = 1;
  private static final int SIZE_OF_UINT16 = 2;

  private static final int SIZE_OF_OPTIONS = SIZE_OF_UINT8;
  private static final int SIZE_OF_TYPE = SIZE_OF_UINT16;

  private String senderConnectionId;

  private final int messageTypeId;
  private final NetworkMessageOptions options;
  private final byte[] payload;

  private NetworkMessage(int messageTypeId, NetworkMessageOptions options, byte[] payload) {
    this.messageTypeId = messageTypeId;
    this.options = options;
    this.payload = payload;
  }

  public NetworkMessage(int messageTypeId, NetworkMessageOptions options, NetworkSerializable payload) {
    this.messageTypeId = messageTypeId;
    this.options = options;
    this.payload = payload.serialize();
  }

  public NetworkMessage(int messageTypeId, NetworkSerializable payload) {
    this.messageTypeId = messageTypeId;
    
    // For now, disable compression by default - can be enabled via NetworkMessageOptions constructor
    this.options = NetworkMessageOptions.builder().compressed(false).build();
    
    // Serialize payload
    this.payload = payload.serialize();
  }

  public int length() {
    return SIZE_OF_TYPE + SIZE_OF_OPTIONS + payload.length;
  }

  public byte[] toBytes() throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    DataOutputStream data = new DataOutputStream(out);

    // Calculate expected length and verify it
    int expectedLength = length();
    int actualLength = SIZE_OF_TYPE + SIZE_OF_OPTIONS + payload.length;

    if (expectedLength != actualLength) {
      throw new IllegalArgumentException("Serialized message length mismatch. Expected: " + expectedLength + ", but got: " + actualLength);
    }

    // Write 2-byte length (excluding the length field itself)
    data.writeShort(length());

    // Write 1-byte options
    data.writeByte(options.toByte());

    // Write 2-byte type
    data.writeShort(messageTypeId); // use the numeric ID

    // Write payload
    data.write(payload);

    return out.toByteArray();
  }

  public static NetworkMessage fromBytes(byte[] bytes) throws IOException {
    DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes));

    // total length: size size of options + size of message type + payload.length
    int length = in.readUnsignedShort();
    // the options
    byte optionsByte = in.readByte();
    NetworkMessageOptions options = NetworkMessageOptions.fromByte(optionsByte);

    // the type
    int messageTypeId = in.readUnsignedShort();

    byte[] payload = new byte[length - SIZE_OF_OPTIONS - SIZE_OF_TYPE];
    in.readFully(payload);

    return new NetworkMessage(messageTypeId, options, payload);
  }

  public <T extends NetworkSerializable> T deserialize(Class<T> clazz) throws Exception {
    // Get payload and decompress if needed
    byte[] actualPayload = payload;
    if (options.isCompressed()) {
      try {
        actualPayload = CompressionUtil.decompress(payload);
      } catch (Exception e) {
        throw new RuntimeException("Failed to decompress message payload", e);
      }
    }

    // Create new instance and deserialize using interface method
    T obj = clazz.getDeclaredConstructor().newInstance();
    obj.readFrom(actualPayload);
    return obj;
  }

  public String getSenderConnectionId() {
    return senderConnectionId;
  }

  public void setSenderConnectionId(String senderConnectionId) {
    this.senderConnectionId = senderConnectionId;
  }

  public int getType() {
    return messageTypeId;
  }

  public NetworkMessageOptions getOptions() {
    return options;
  }

  public byte[] getPayload() {
    return payload;
  }

  public String toHexString() {
    StringBuilder sb = new StringBuilder();
    sb.append(String.format("Length: %04X, Options: %02X, Type: %04X, Payload: ", length(), options.toByte(), getType()));
    for (byte b : payload) {
      sb.append(String.format("%02X ", b));
    }
    return sb.toString();
  }
}
