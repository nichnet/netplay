package com.netplay.shared.messages;

import com.netplay.shared.NetworkSerializable;

public interface NetworkMessageHandlerInterface<T extends NetworkSerializable> {
  void handle(String connectionId, T message);
}
