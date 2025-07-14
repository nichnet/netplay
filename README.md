# Netplay

A high-performance Java NIO networking library designed for real-time multiplayer applications and client-server communication.

## Features

- **High Performance**: Built on Java NIO for non-blocking I/O operations
- **Scalable**: Supports thousands of concurrent connections
- **Server-Authoritative**: Designed for secure, server-controlled game logic and state management
- **Secure Architecture**: Client libraries contain no server code, preventing reverse engineering of server logic
- **Reflection-Based Registration**: Messages and actions can be registered using package paths with reflection
- **Custom Protocol**: Implements a binary message protocol for efficient communication
- **Modular Build System**: Separate client and server JAR builds using Gradle

- Documentation
For comprehensive guides, tutorials, and API reference, visit the [Netplay Wiki](https://github.com/nichnet/netplay/wiki).

## Protocol

The library implements a custom binary message protocol optimized for real-time communication. For detailed protocol specification, see: [Message Protocol Documentation]()

## Getting Started

### Building the Library

The project uses Gradle with specialized tasks for building client and server components:

```bash
# Build client library (server code excluded)
./gradlew jarClient

# Build server library (complete functionality)
./gradlew jarServer
```

### Running the Chat Example

A complete chat system example is included to demonstrate the library's capabilities:

```bash
# Start the example server
./gradlew exampleServer

# Start the example client
./gradlew exampleClient
```

The chat example showcases:
- Client connection management
- Username registration
- Real-time messaging
- System notifications (join/leave events)
- Multi-user chat rooms

## Architecture

### Recommended Package Structure

For optimal organization and security, structure your project packages as follows:

```
com.yourapp.shared/
├── network/
    ├── actions/
    └── messages/
└── whatever../

com.yourapp.server/
├── network/
    ├── actions/
    ├── messages/
    └── GameServer.java
└── whatever../

com.yourapp.client/
├── network/
    ├── actions/
    ├── messages/
    └── GameClient.java
└── whatever../
```

### Message Registration

Register your message and action packages using reflection:

```java
// Server registration
public class GameServer extends Server {
  public GameServer() {
    MessageRegistry.registerByPackage("com.yourapp.shared.network.messages", "com.yourapp.server.network.messages");
    ActionRegistry.registerByPackage("com.yourapp.shared.network.actions", "com.yourapp.server.network.actions");
  }
}
```

```java
// Client registration
public class GameServer extends Client {
  public GameServer() {
    MessageRegistry.registerByPackage("com.yourapp.shared.network.messages", "com.yourapp.client.network.messages");
    ActionRegistry.registerByPackage("com.yourapp.shared.network.actions", "com.yourapp.client.network.actions");
  }
}
```

### Client-Server Separation

- **Client JAR**: Contains only client-side and shared networking code
- **Server JAR**: Contains full server implementation with connection management
- **Important**: Exclude server packages from client builds to prevent server logic exposure

## Requirements

- Java 8 or higher
- Gradle (for building)

## Contributing

Issues and feature requests are welcome! Please feel free to:
- Report bugs or performance issues
- Suggest new features or improvements
- Submit questions about implementation

## Support

For questions, issues, or feature requests, please use the GitHub Issues system. While we welcome community input, please note that response times and feature implementation are not guaranteed.

## License

This project is distributed under a custom license:

- ✅ **Permitted**: Use in your applications (commercial or non-commercial with) with attribution
- ✅ **Permitted**: Modify for your own use
- ❌ **Not Permitted**: Redistribution of source code or modified versions
- ❌ **Not Permitted**: Publishing forks or derivatives

**Recommended Usage**: Use the pre-built JARs provided in releases rather than building from source.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
