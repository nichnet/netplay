package com.netplay.shared.actions;

import com.netplay.shared.messages.NetworkMessage;

import java.util.HashMap;
import java.util.Map;

public abstract class NetworkActions {
  private final Map<Integer, NetworkAction> actionHandlers = new HashMap<>();

  protected void registerAction(NetworkAction action) {
    if(actionHandlers.containsKey(action.getId())) {
      System.out.println("Unable to register action for id '" + action.getId() + "' because its already registered.");
      return;
    }
    actionHandlers.put(action.getId(), action);
  }

  public void handleReceivedMessage(NetworkMessage networkMessage) {
    NetworkAction action = actionHandlers.get(networkMessage.getType());
    if (action == null) {
      System.out.println("No action handler registered for message type: " + networkMessage.getType());
      return;
    }

    try {
      action.perform(networkMessage);
    } catch (Exception e) {
      System.err.println("Error performing action " + networkMessage.getType() + " for connection " + networkMessage.getSenderConnectionId() + ": " + e.getMessage());
      e.printStackTrace();
    }
  }
}
