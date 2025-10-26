package com.netplay.shared;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for chunking large messages into smaller packets that fit within the buffer size.
 * Provides transparent chunking and reassembly - the application layer is unaware of chunking.
 */
public class MessageChunker {
  // Chunk size: 1024 bytes total (1 byte header + 1023 bytes data)
  private static final int CHUNK_SIZE = NetworkConstants.BUFFER_SIZE;
  private static final int CHUNK_DATA_SIZE = CHUNK_SIZE - 1; // Reserve 1 byte for header

  // Chunk header flags
  private static final byte FLAG_FIRST_CHUNK_ONLY = 0x00;      // 0b00000000 - single chunk message
  private static final byte FLAG_FIRST_CHUNK_MORE = 0x40;      // 0b01000000 - first chunk, more follow
  private static final byte FLAG_CONTINUATION_MORE = (byte)0xC0; // 0b11000000 - continuation, more follow
  private static final byte FLAG_CONTINUATION_FINAL = (byte)0x80; // 0b10000000 - continuation, final

  /**
   * Split a message into chunks if it exceeds the chunk data size.
   * Each chunk has format: [1 byte header][up to 1023 bytes data]
   *
   * @param messageBytes the complete message bytes to chunk
   * @return list of chunks (each chunk is ready to send on the wire)
   */
  public static List<byte[]> chunkMessage(byte[] messageBytes) {
    List<byte[]> chunks = new ArrayList<>();
    int messageLength = messageBytes.length;

    // If message fits in one chunk, send as-is with simple header
    if (messageLength <= CHUNK_DATA_SIZE) {
      byte[] chunk = new byte[messageLength + 1];
      chunk[0] = FLAG_FIRST_CHUNK_ONLY;
      System.arraycopy(messageBytes, 0, chunk, 1, messageLength);
      chunks.add(chunk);
      return chunks;
    }

    // Message needs to be split into multiple chunks
    int offset = 0;
    boolean isFirst = true;

    while (offset < messageLength) {
      int remaining = messageLength - offset;
      int chunkDataLength = Math.min(remaining, CHUNK_DATA_SIZE);
      boolean isLast = (offset + chunkDataLength) >= messageLength;

      byte[] chunk = new byte[chunkDataLength + 1];

      // Set header byte based on position
      if (isFirst && !isLast) {
        chunk[0] = FLAG_FIRST_CHUNK_MORE;
      } else if (!isFirst && !isLast) {
        chunk[0] = FLAG_CONTINUATION_MORE;
      } else if (!isFirst && isLast) {
        chunk[0] = FLAG_CONTINUATION_FINAL;
      }

      // Copy data
      System.arraycopy(messageBytes, offset, chunk, 1, chunkDataLength);
      chunks.add(chunk);

      offset += chunkDataLength;
      isFirst = false;
    }

    return chunks;
  }

  /**
   * Checks if a chunk header indicates this is a continuation chunk.
   * @param header the chunk header byte
   * @return true if this is a continuation chunk (not the first chunk)
   */
  public static boolean isContinuation(byte header) {
    return (header & 0x80) != 0; // Bit 7 set = continuation
  }

  /**
   * Checks if a chunk header indicates more chunks follow.
   * @param header the chunk header byte
   * @return true if more chunks follow this one
   */
  public static boolean hasMore(byte header) {
    return (header & 0x40) != 0; // Bit 6 set = more chunks follow
  }

  /**
   * Extract the data portion from a chunk (removes the header byte).
   * @param chunk the complete chunk including header
   * @return the data portion (everything after the header byte)
   */
  public static byte[] extractChunkData(byte[] chunk) {
    if (chunk.length <= 1) {
      return new byte[0];
    }
    byte[] data = new byte[chunk.length - 1];
    System.arraycopy(chunk, 1, data, 0, chunk.length - 1);
    return data;
  }
}
