package com.netplay.shared.messages;

import com.netplay.shared.CompressionUtil;
import com.netplay.shared.NetworkSerializable;
import org.reflections.Reflections;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class NetworkMessageRegistry {
  private static NetworkMessageRegistry instance;
  private static final Map<Integer, Class<? extends NetworkSerializable>> registry = new HashMap<>();

  public NetworkMessageRegistry(String... packagesToScan) throws Exception {
    instance = this;
    initialize(packagesToScan);
  }

  private void initialize(String... packagesToScan) throws Exception {
    for(String packageToScan : packagesToScan){
      System.out.println("Starting NetworkMessageRegistry initialization for package: " + packageToScan);
      Reflections reflections = new Reflections(packageToScan);
      System.out.println("Reflections object created, scanning for @NetworkMessageHandler annotations...");

      // Find all classes annotated with @NetworkMessageHandler
      Set<Class<?>> annotatedClasses = reflections.getTypesAnnotatedWith(NetworkMessageHandler.class);
      System.out.println("Found " + annotatedClasses.size() + " annotated classes");

      for (Class<?> clazz : annotatedClasses) {
        if (!NetworkSerializable.class.isAssignableFrom(clazz)) {
          throw new RuntimeException("Class " + clazz.getName() + " annotated with @NetworkMessageHandler but does not extend NetworkSerializable");
        }

        NetworkMessageHandler annotation = clazz.getAnnotation(NetworkMessageHandler.class);
        int messageTypeId = annotation.value();

        @SuppressWarnings("unchecked")
        Class<? extends NetworkSerializable> messageClass = (Class<? extends NetworkSerializable>) clazz;

        // Check for static deserialize method
        Method deserializeMethod = messageClass.getMethod("deserialize", byte[].class);

        registry.put(messageTypeId, messageClass);
        System.out.println("Registered message type id: '" + messageTypeId + "' with class " + clazz.getName());
      }
    }
  }

  public static <T extends NetworkSerializable> T create(NetworkMessage networkMessage) throws Exception {
    Class<? extends NetworkSerializable> clazz = registry.get(networkMessage.getType());
    if (clazz == null) {
      throw new IllegalArgumentException("Unknown message type: " + networkMessage.getType());
    }

    // Get payload and decompress if needed
    byte[] payload = networkMessage.getPayload();
    if (networkMessage.getOptions().isCompressed()) {
      try {
        payload = CompressionUtil.decompress(payload);
      } catch (Exception e) {
        throw new RuntimeException("Failed to decompress message payload", e);
      }
    }

    Method deserializeMethod = clazz.getMethod("deserialize", byte[].class);
    @SuppressWarnings("unchecked")
    T obj = (T) deserializeMethod.invoke(null, payload);
    return obj;
  }

  public static NetworkMessageRegistry getInstance() {
    return instance;
  }
}

