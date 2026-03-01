# Netplay

A lightweight Java NIO networking library designed for real-time multiplayer applications and client-server communication.

## Features

- **High Performance**: Built on Java NIO for non-blocking I/O operations
- **Zero Reflection**: No runtime reflection or annotation scanning
- **Pluggable Serialization**: Bring your own serializer or use the included JSON serializer
- **Simple API**: Explicit handler registration with a clean, static API
- **Class-Based Routing**: Messages routed by class name, no integer type IDs needed
- **Modular Build System**: Separate client and server JAR builds using Gradle

## Wire Format

Messages use a simple newline-delimited text format:
```
ClassName\n
{"json":"body"}\n
```

## Getting Started

### Building the Library

```bash
# Build client library
./gradlew clientJar

# Build server library
./gradlew serverJar

# Build both libraries
./gradlew build
```

### Basic Usage

#### 1. Define Message Classes

Simple POJOs - no interfaces or annotations required:

```java
public class ChatMessage {
    private String sender;
    private String message;

    public ChatMessage() {}

    public ChatMessage(String sender, String message) {
        this.sender = sender;
        this.message = message;
    }

    public String getSender() { return sender; }
    public String getMessage() { return message; }
}
```

#### 2. Configure and Register Handlers

```java
// Set up serializer (do this once at startup)
Network.setSerializer(new JsonSerializer());

// Register message handlers
Network.on(ChatMessage.class, (msg, senderId) -> {
    System.out.println(msg.getSender() + ": " + msg.getMessage());
});
```

#### 3. Send Messages

```java
// From client to server
client.send(new ChatMessage("Alice", "Hello!"));

// From server to specific client
Network.send(connectionId, new ChatMessage("Server", "Welcome!"));

// Broadcast to all clients
Network.broadcast(new ChatMessage("Server", "Announcement"));

// Broadcast to all except specific clients
Network.broadcastExcept(new String[]{senderId}, message);
```

### Server Implementation

```java
public class MyServer extends Server {
    @Override
    public void onUserConnected(NetworkConnection connection) {
        System.out.println("User connected: " + connection.getId());
    }

    @Override
    public void onUserDisconnected(NetworkConnection connection) {
        System.out.println("User disconnected: " + connection.getId());
    }
}

// Start the server
Network.setSerializer(new JsonSerializer());
MyServer server = new MyServer();
server.start("localhost", 8080);
```

### Client Implementation

```java
public class MyClient extends Client {
    @Override
    public void onConnected() {
        System.out.println("Connected to server");
        send(new LoginMessage("username"));
    }

    @Override
    public void onDisconnected() {
        System.out.println("Disconnected from server");
    }
}

// Connect the client
Network.setSerializer(new JsonSerializer());
MyClient client = new MyClient();
client.connect("localhost", 8080);
```

### Chat Example

A complete chat example is included:

```bash
# Build and run the chat server
./gradlew chatServerJar
java -jar build/libs/chat-server-1.0.0.jar

# Build and run the chat client (in another terminal)
./gradlew chatClientJar
java -jar build/libs/chat-client-1.0.0.jar
```

## Custom Serializers

Implement the `Serializer` interface to use your own serialization:

```java
public class MySerializer implements Serializer {
    @Override
    public byte[] serialize(Object obj) {
        // Your serialization logic
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        // Your deserialization logic
    }
}

Network.setSerializer(new MySerializer());
```

## Requirements

- Java 11 or higher
- Gradle (for building)
- Gson (included as dependency)

## Message Size Limits

- **Server (outgoing)**: No size limit. The server can send messages of any size to clients.
- **Client (incoming)**: 8KB read buffer. Large messages are accumulated incrementally, so there is no practical limit on incoming message size.
- **Client (outgoing)**: No size limit. Messages are written directly to the socket.

This design allows the server to send large payloads (e.g., room data with hundreds of tiles) without chunking.

## Key Classes

- **`Network`**: Static API for handler registration and message sending
- **`Serializer`**: Interface for pluggable serialization
- **`JsonSerializer`**: Gson-based serializer implementation
- **`Server`**: Abstract base class for server implementation
- **`Client`**: Abstract base class for client implementation
- **`NetworkConnection`**: Represents a client connection on the server

## Contributing

Issues and feature requests are welcome!

## License

This project is distributed under a custom license:

- Permitted: Use in your applications (commercial or non-commercial) with attribution
- Permitted: Modify for your own use
- Not Permitted: Redistribution of source code or modified versions
- Not Permitted: Publishing forks or derivatives

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
