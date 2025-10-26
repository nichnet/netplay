package com.netplay.shared;

public class NetworkConstants {
  public static final int BUFFER_SIZE = 1024;

  // Maximum message size: 65535 bytes (max value of unsigned short) + 2 bytes for length prefix
  public static final int MAX_MESSAGE_SIZE = 65537;

  public static String generateId() {
    return Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE))
      + Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE))
      + Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE))
      + Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE));
  }
}
