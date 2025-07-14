package com.netplay.shared;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

public abstract class NetworkSerializable {
  public byte[] serialize() {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      DataOutputStream data = new DataOutputStream(out);

      Arrays.stream(this.getClass().getDeclaredMethods())
        .filter(m -> m.isAnnotationPresent(NetworkSerializableProperty.class) && m.getParameterCount() == 0)
        .sorted(Comparator.comparingInt(m -> m.getAnnotation(NetworkSerializableProperty.class).value()))
        .forEach(method -> {
          try {
            method.setAccessible(true);
            Object value = method.invoke(this);
            writeValue(data, value);
          } catch (Exception e) {
            throw new RuntimeException("Failed to serialize property: " + method.getName(), e);
          }
        });

      // TODO: only serializing methods for now.
      // Serialize fields
      // for (Field field : this.getClass().getDeclaredFields()) {
      //   if (field.isAnnotationPresent(NetworkSerializable.class)) {
      //     field.setAccessible(true);
      //     Object value = field.get(this);
      //     writeValue(data, value);
      //   }
      // }

      return out.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private void writeValue(DataOutputStream out, Object value) throws IOException {
    if (value instanceof Integer) {
      out.writeInt((int) value);
    } else if (value instanceof Short) {
      out.writeShort((short) value);
    } else if (value instanceof Long) {
      out.writeLong((long) value);
    } else if (value instanceof Float) {
      out.writeFloat((float) value);
    } else if (value instanceof Boolean) {
      out.writeBoolean((boolean) value);
    } else if (value instanceof String) {
      writeUTFString(out, (String) value);
    } else if (value instanceof Byte) { // TODO will a Byte work its not primitive byte? check!
      out.writeByte((byte) value);
    } else if (value.getClass().isArray()) {
      writeArray(out, value);
    } else if (value instanceof NetworkSerializable) {
      // Handle nested NetworkSerializable objects
      out.writeBoolean(true);
      NetworkSerializable nested = (NetworkSerializable) value;
      byte[] nestedData = nested.serialize();
      out.writeShort(nestedData.length); // Write length of nested object data
      out.write(nestedData); // Write the nested object data
    } else if (value == null) {
      // Decide on a protocol for this.
      throw new RuntimeException("TODO: Null values are not currently supported");
    } else {
      throw new RuntimeException("Unsupported value type: " + value.getClass());
    }
  }

  private void writeArray(DataOutputStream out, Object value) throws IOException {
    // Handle arrays of non-byte data (like int[], String[], etc.)
    int length = Array.getLength(value);
    out.writeShort(length); // Write array length
    for (int i = 0; i < length; i++) {
      Object element = Array.get(value, i);
      writeValue(out, element);  // Recursively serialize each element
    }
  }

  // public abstract void readFrom(byte[] data) throws Exception;

  //-------------------------------------------------------------------

  public void readFrom(byte[] data) throws Exception {
    DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));

    // Get all annotated methods sorted by their value (index)
    Method[] methods = this.getClass().getDeclaredMethods();
    Arrays.stream(methods)
      .filter(m -> m.isAnnotationPresent(NetworkSerializableProperty.class) && m.getParameterCount() == 0)
      .sorted(Comparator.comparingInt(m -> m.getAnnotation(NetworkSerializableProperty.class).value()))
      .forEach(method -> {
        try {
          // Find corresponding setter method
          String propertyName = getPropertyName(method); // getSender -> sender
          Field field = this.getClass().getDeclaredField(propertyName);
          field.setAccessible(true);

          // Read value based on field type
          Object value = readValueByType(input, field.getType());
        field.set(this, value);
        } catch (Exception e) {
          throw new RuntimeException("Failed to deserialize property: " + method.getName(), e);
        }
      });
  }

  private String getPropertyName(Method getter) {
    String methodName = getter.getName();
    if (methodName.startsWith("get")) {
      return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
    }

    // Handle other naming conventions if needed
    return methodName;
  }

  private Object readValueByType(DataInputStream input, Class<?> type) throws Exception {
    if (type == String.class) {
      return readUTFString(input);
    } else if (type == int.class || type == Integer.class) {
      return input.readInt();
    } else if (type == short.class || type == Short.class) {
      return input.readShort();
    } else if (type == long.class || type == Long.class) {
      return input.readLong();
    } else if (type == float.class || type == Float.class) {
      return input.readFloat();
    } else if (type == boolean.class || type == Boolean.class) {
      return input.readBoolean();
    } else if (type == byte.class || type == Byte.class) {
      return input.readByte();
    } else if (type.isArray()) {
      return readArray(input, type);
    } else if (NetworkSerializable.class.isAssignableFrom(type)) {
      // Handle nested NetworkSerializable objects
      boolean hasNestedObject = input.readBoolean();
      if (hasNestedObject) {
        int nestedDataLength = input.readUnsignedShort();
        byte[] nestedData = new byte[nestedDataLength];
        input.readFully(nestedData);

        // Use reflection to call the deserialize method on the nested object
        Method deserializeMethod = type.getMethod("deserialize", byte[].class);
        return deserializeMethod.invoke(null, nestedData);
      }
      return null;
    } else {
      // TODO also handle null type as its own case.
      throw new UnsupportedOperationException("Unsupported type: " + type);
    }
  }

  private Object readArray(DataInputStream input, Class<?> arrayType) throws Exception {
    int length = input.readUnsignedShort();
    Class<?> componentType = arrayType.getComponentType();
    Object array = Array.newInstance(componentType, length);

    for (int i = 0; i < length; i++) {
      Object element = readValueByType(input, componentType);
      Array.set(array, i, element);
    }

    return array;
  }

  //-------------------------------------------------------------------

  private void writeUTFString(DataOutputStream out, String str) throws IOException {
    byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
    out.writeShort(bytes.length);
    out.write(bytes);
  }

  protected String readUTFString(DataInputStream in) throws IOException {
    int length = in.readUnsignedShort();  // Read length of the string
    byte[] bytes = new byte[length];      // Create byte array of that length
    in.readFully(bytes);                  // Read the bytes into the array
    return new String(bytes, StandardCharsets.UTF_8); // Convert bytes to string using UTF-8 encoding
  }

  // Deserialize an array of objects from byte data
  protected <T extends NetworkSerializable> T[] deserializeArray(Class<T> clazz, byte[] data) throws Exception {
    ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
    DataInputStream dataIn = new DataInputStream(byteStream);

    // First, read the number of elements in the array
    int arrayLength = dataIn.readUnsignedShort(); // Read the number of items in the array

    // Create an array to hold the deserialized objects
    @SuppressWarnings("unchecked")
    T[] array = (T[]) Array.newInstance(clazz, arrayLength);

    // Deserialize each item in the array
    for (int i = 0; i < arrayLength; i++) {
      // Read the length of the current item
      int itemLength = dataIn.readUnsignedShort(); // Length of the item (e.g., ChatMessage)

      // Create a byte array to hold the item data
      byte[] itemData = new byte[itemLength];

      // Read the item data into the byte array
      dataIn.readFully(itemData);

      // Deserialize the item (e.g., ChatMessage) using the provided class's static deserialize method
      array[i] = (T) clazz.getMethod("deserialize", byte[].class).invoke(null, itemData);
    }

    return array;
  }
}
