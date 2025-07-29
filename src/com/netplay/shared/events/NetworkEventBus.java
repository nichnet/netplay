package com.netplay.shared.events;

import com.netplay.shared.messages.NetworkMessage;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

public class NetworkEventBus {
  private static NetworkEventBus instance;
  private final Map<Integer, List<NetworkEventHandlerMethod>> handlers;

  private static class NetworkEventHandlerMethod {
    final Object instance;
    final Method method;

    NetworkEventHandlerMethod(Object instance, Method method) {
      this.instance = instance;
      this.method = method;
    }
  }

  public NetworkEventBus() {
    if (instance == null) {
      instance = this;
    }
    this.handlers = new ConcurrentHashMap<>();
  }

  /**
   * Register event handlers for a specific instance
   */
  public synchronized void registerHandler(Object handlerInstance) {
    Class<?> clazz = handlerInstance.getClass();

    for (Method method : clazz.getDeclaredMethods()) {
      if (method.isAnnotationPresent(NetworkEventHandler.class)) {
        NetworkEventHandler annotation = method.getAnnotation(NetworkEventHandler.class);
        int messageType = annotation.value();

        // Validate method signature - should take NetworkMessage parameter
        if (method.getParameterCount() != 1 ||
                !method.getParameterTypes()[0].isAssignableFrom(NetworkMessage.class)) {
          throw new IllegalArgumentException(
                  "Event handler method " + method.getName() + " in " + clazz.getName() +
                          " must have exactly one parameter of type NetworkMessage"
          );
        }

        method.setAccessible(true);

        // Add to handlers map
        handlers.computeIfAbsent(messageType, k -> new ArrayList<>())
                .add(new NetworkEventHandlerMethod(handlerInstance, method));
      }
    }
  }

  /**
   * Register all event handlers in the specified packages using reflection
   */
  public synchronized void registerHandlers(String... packageNames) {
    try {
      for (String packageName : packageNames) {
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .forPackage(packageName)
                .addScanners(new MethodAnnotationsScanner(), new TypeAnnotationsScanner(), new SubTypesScanner()));

        Set<Method> annotatedMethods = reflections.getMethodsAnnotatedWith(NetworkEventHandler.class);
        Set<Class<?>> allClasses = new HashSet<>();

        // Get all classes that have methods with @NetworkEventHandler annotation
        for (Method method : annotatedMethods) {
          Class<?> declaringClass = method.getDeclaringClass();
          // Only include classes that are actually in the specified package
          if (declaringClass.getPackage() != null && declaringClass.getPackage().getName().startsWith(packageName)) {
            allClasses.add(declaringClass);
          }
        }

        // If that doesn't work, try scanning all classes manually
        if (allClasses.isEmpty()) {
          Set<Class<?>> allClassesFromSubTypes = reflections.getSubTypesOf(Object.class);
          
          for (Class<?> clazz : allClassesFromSubTypes) {
            // Only check classes that are actually in the specified package
            if (clazz.getPackage() != null && clazz.getPackage().getName().startsWith(packageName)) {
              Method[] methods = clazz.getDeclaredMethods();
              for (Method method : methods) {
                if (method.isAnnotationPresent(NetworkEventHandler.class)) {
                  allClasses.add(clazz);
                  break;
                }
              }
            }
          }
        }

        for (Class<?> clazz : allClasses) {
          // Skip abstract classes and interfaces
          if (clazz.isInterface() || java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
            continue;
          }

          try {
            // Try to create instance - assume default constructor
            Object instance = clazz.getDeclaredConstructor().newInstance();
            registerHandler(instance);
          } catch (Exception e) {
            System.err.println("Failed to create instance of " + clazz.getName() +
                    ". Event handlers in this class will be ignored: " + e.getMessage());
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to register event handlers", e);
    }
  }


  public void handleReceivedMessage(NetworkMessage networkMessage) {
    List<NetworkEventHandlerMethod> messageHandlers = handlers.get(networkMessage.getType());

    if (messageHandlers == null || messageHandlers.isEmpty()) {
      System.out.println("No network event handler registered for message type: " + networkMessage.getType());
      return;
    }

    for (NetworkEventHandlerMethod handler : messageHandlers) {
      try {
        handler.method.invoke(handler.instance, networkMessage);
      } catch (Exception e) {
        System.err.println("Error invoking handler " + handler.method.getName() + 
                " for message type " + networkMessage.getType() + 
                " from connection " + networkMessage.getSenderConnectionId() + ": " + e.getMessage());
        e.printStackTrace();
      }
    }
  }

  public static NetworkEventBus getInstance() {
    if (instance == null) {
      instance = new NetworkEventBus();
    }
    return instance;
  }
}
