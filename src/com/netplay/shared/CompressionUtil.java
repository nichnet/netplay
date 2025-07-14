package com.netplay.shared;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class CompressionUtil {
    
    public static byte[] compress(byte[] data) throws IOException {
        if (data == null || data.length == 0) {
            return data;
        }
        
        Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        
        while (!deflater.finished()) {
            int compressedSize = deflater.deflate(buffer);
            outputStream.write(buffer, 0, compressedSize);
        }
        
        deflater.end();
        return outputStream.toByteArray();
    }
    
    public static byte[] decompress(byte[] compressedData) throws IOException {
        if (compressedData == null || compressedData.length == 0) {
            return compressedData;
        }
        
        Inflater inflater = new Inflater();
        inflater.setInput(compressedData);
        
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        
        try {
            while (!inflater.finished()) {
                int decompressedSize = inflater.inflate(buffer);
                outputStream.write(buffer, 0, decompressedSize);
            }
        } catch (Exception e) {
            throw new IOException("Failed to decompress data", e);
        } finally {
            inflater.end();
        }
        
        return outputStream.toByteArray();
    }
}