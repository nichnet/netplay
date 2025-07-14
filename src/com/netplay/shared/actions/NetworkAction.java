package com.netplay.shared.actions;

import com.netplay.shared.messages.NetworkMessage;

public abstract class NetworkAction {
  private int id;

  public NetworkAction(int id) {
    this.id = id;
  }

  public abstract void perform(NetworkMessage message) throws Exception;

  public int getId() {
    return id;
  }
}
