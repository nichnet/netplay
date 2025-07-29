package com.netplay.shared;

import java.io.*;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Utility class for serializing and deserializing NetworkSerializable objects.
 * Moved from the abstract class to support interface-based approach that allows
 * objects with different inheritance hierarchies to be serialized.
 */
public class NetworkSerializer {

    /**
     * Serialize a NetworkSerializable object to byte array using reflection.
     * @param obj the object to serialize
     * @return byte array representation
     */
    public static byte[] serialize(NetworkSerializable obj) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            DataOutputStream data = new DataOutputStream(out);

            Arrays.stream(obj.getClass().getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(NetworkSerializableProperty.class) && m.getParameterCount() == 0)
                .sorted(Comparator.comparingInt(m -> m.getAnnotation(NetworkSerializableProperty.class).value()))
                .forEach(method -> {
                    try {
                        method.setAccessible(true);
                        Object value = method.invoke(obj);
                        writeValue(data, value);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to serialize property: " + method.getName(), e);
                    }
                });

            return out.toByteArray();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Deserialize byte data into a NetworkSerializable object.
     * @param obj the object to populate with deserialized data
     * @param data the byte array to deserialize from
     * @throws Exception if deserialization fails
     */
    public static void deserialize(NetworkSerializable obj, byte[] data) throws Exception {
        DataInputStream input = new DataInputStream(new ByteArrayInputStream(data));

        // Get all annotated methods sorted by their value (index)
        Method[] methods = obj.getClass().getDeclaredMethods();
        Arrays.stream(methods)
            .filter(m -> m.isAnnotationPresent(NetworkSerializableProperty.class) && m.getParameterCount() == 0)
            .sorted(Comparator.comparingInt(m -> m.getAnnotation(NetworkSerializableProperty.class).value()))
            .forEach(method -> {
                try {
                    // Find corresponding setter method
                    String propertyName = getPropertyName(method); // getSender -> sender
                    Field field = obj.getClass().getDeclaredField(propertyName);
                    field.setAccessible(true);

                    // Read value based on field type
                    Object value = readValueByType(input, field.getType());
                    field.set(obj, value);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize property: " + method.getName(), e);
                }
            });
    }

    /**
     * Deserialize an array of NetworkSerializable objects from byte data.
     * @param clazz the class type of the objects in the array
     * @param data the byte array to deserialize from
     * @return array of deserialized objects
     * @throws Exception if deserialization fails
     */
    @SuppressWarnings("unchecked")
    public static <T extends NetworkSerializable> T[] deserializeArray(Class<T> clazz, byte[] data) throws Exception {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(data);
        DataInputStream dataIn = new DataInputStream(byteStream);

        // First, read the number of elements in the array
        int arrayLength = dataIn.readUnsignedShort(); // Read the number of items in the array

        // Create an array to hold the deserialized objects
        T[] array = (T[]) Array.newInstance(clazz, arrayLength);

        // Deserialize each item in the array
        for (int i = 0; i < arrayLength; i++) {
            // Read the length of the current item
            int itemLength = dataIn.readUnsignedShort(); // Length of the item (e.g., ChatMessage)

            // Create a byte array to hold the item data
            byte[] itemData = new byte[itemLength];

            // Read the item data into the byte array
            dataIn.readFully(itemData);

            // Deserialize the item using the provided class's static deserialize method
            array[i] = (T) clazz.getMethod("deserialize", byte[].class).invoke(null, itemData);
        }

        return array;
    }

    private static void writeValue(DataOutputStream out, Object value) throws IOException {
        if (value == null) {
            out.writeBoolean(false); // Write false to indicate null value
        } else {
            out.writeBoolean(true); // Write true to indicate non-null value
            
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
            } else if (value instanceof Byte) {
                out.writeByte((byte) value);
            } else if (value.getClass().isArray()) {
                writeArray(out, value);
            } else if (value instanceof NetworkSerializable) {
                // Handle nested NetworkSerializable objects
                NetworkSerializable nested = (NetworkSerializable) value;
                byte[] nestedData = nested.serialize();
                out.writeShort(nestedData.length); // Write length of nested object data
                out.write(nestedData); // Write the nested object data
            } else {
                throw new RuntimeException("Unsupported value type: " + value.getClass());
            }
        }
    }

    private static void writeArray(DataOutputStream out, Object value) throws IOException {
        // Handle arrays of non-byte data (like int[], String[], etc.)
        int length = Array.getLength(value);
        out.writeShort(length); // Write array length
        for (int i = 0; i < length; i++) {
            Object element = Array.get(value, i);
            writeValue(out, element);  // Recursively serialize each element
        }
    }

    private static String getPropertyName(Method getter) {
        String methodName = getter.getName();
        if (methodName.startsWith("get")) {
            return methodName.substring(3, 4).toLowerCase() + methodName.substring(4);
        }

        // Handle other naming conventions if needed
        return methodName;
    }

    private static Object readValueByType(DataInputStream input, Class<?> type) throws Exception {
        // First check if the value is null by reading the boolean flag
        if (!input.readBoolean()) {
            return null; // Value was null
        }
        
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
            int nestedDataLength = input.readUnsignedShort();
            byte[] nestedData = new byte[nestedDataLength];
            input.readFully(nestedData);

            // Use reflection to call the deserialize method on the nested object
            Method deserializeMethod = type.getMethod("deserialize", byte[].class);
            return deserializeMethod.invoke(null, nestedData);
        } else {
            throw new UnsupportedOperationException("Unsupported type: " + type);
        }
    }

    private static Object readArray(DataInputStream input, Class<?> arrayType) throws Exception {
        int length = input.readUnsignedShort();
        Class<?> componentType = arrayType.getComponentType();
        Object array = Array.newInstance(componentType, length);

        for (int i = 0; i < length; i++) {
            Object element = readValueByType(input, componentType);
            Array.set(array, i, element);
        }

        return array;
    }

    private static void writeUTFString(DataOutputStream out, String str) throws IOException {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String readUTFString(DataInputStream in) throws IOException {
        int length = in.readUnsignedShort();  // Read length of the string
        byte[] bytes = new byte[length];      // Create byte array of that length
        in.readFully(bytes);                  // Read the bytes into the array
        return new String(bytes, StandardCharsets.UTF_8); // Convert bytes to string using UTF-8 encoding
    }
}