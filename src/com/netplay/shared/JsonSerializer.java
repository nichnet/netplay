package com.netplay.shared;

import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;

/**
 * JSON-based serializer implementation using Gson.
 */
public class JsonSerializer implements Serializer {
    private final Gson gson = new Gson();

    @Override
    public byte[] serialize(Object obj) {
        return gson.toJson(obj).getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        return gson.fromJson(new String(data, StandardCharsets.UTF_8), clazz);
    }
}
