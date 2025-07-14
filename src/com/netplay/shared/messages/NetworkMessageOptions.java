package com.netplay.shared.messages;

public class NetworkMessageOptions {
  public static final int COMPRESSED_BIT = 0;
  public static final int ENCRYPTED_BIT = 1;

  private final byte options;

  // Private constructor - only builder can create instances
  private NetworkMessageOptions(byte options) {
    this.options = options;
  }

  // Static factory method to get builder
  public static Builder builder() {
    return new Builder();
  }

  // Convert to byte for use in NetworkMessage
  public byte toByte() {
    return options;
  }

  // Create NetworkMessageOptions from a byte (for received messages)
  public static NetworkMessageOptions fromByte(byte options) {
    return new NetworkMessageOptions(options);
  }

  // Utility methods for reading flags
  public boolean isCompressed() {
    return (options & (1 << COMPRESSED_BIT)) != 0;
  }

  public boolean isEncrypted() {
    return (options & (1 << ENCRYPTED_BIT)) != 0;
  }

  // Static utility methods for reading from raw bytes
  public static boolean isCompressed(byte options) {
    return (options & (1 << COMPRESSED_BIT)) != 0;
  }

  public static boolean isEncrypted(byte options) {
    return (options & (1 << ENCRYPTED_BIT)) != 0;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder("Options[");
    if (isCompressed()) sb.append("COMPRESSED ");
    if (isEncrypted()) sb.append("ENCRYPTED ");
    if (sb.length() == 8) sb.append("NONE"); // No flags set
    sb.append("]");
    return sb.toString();
  }

  // Nested Builder class
  public static class Builder {
    private byte options = 0;

    private Builder() {} // Package private constructor

    public Builder compressed(boolean compressed) {
      if (compressed) {
        options |= (1 << COMPRESSED_BIT);
      } else {
        options &= ~(1 << COMPRESSED_BIT);
      }
      return this;
    }

    public Builder encrypted(boolean encrypted) {
      if (encrypted) {
        options |= (1 << ENCRYPTED_BIT);
      } else {
        options &= ~(1 << ENCRYPTED_BIT);
      }
      return this;
    }

    // Convenience methods for enabling flags
    public Builder compressed() {
      return compressed(true);
    }

    public Builder encrypted() {
      return encrypted(true);
    }

    public Builder none() {
      options = 0;
      return this;
    }

    // Build the final NetworkMessageOptions instance
    public NetworkMessageOptions build() {
      return new NetworkMessageOptions(options);
    }
  }
}
