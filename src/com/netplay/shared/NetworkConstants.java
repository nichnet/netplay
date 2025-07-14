package com.netplay.shared;

public class NetworkConstants {
  public static final int BUFFER_SIZE = 1024;

  public static String generateId() {
    return Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE))
      + Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE))
      + Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE))
      + Integer.toHexString((int) (Math.random() * Integer.MAX_VALUE));
  }
}
