package com.netplay.example.shared.messages;

import com.netplay.example.shared.Constants;
import com.netplay.shared.NetworkSerializable;
import com.netplay.shared.NetworkSerializableProperty;
import com.netplay.shared.messages.NetworkMessageHandler;

@NetworkMessageHandler(Constants.NETWORK_MESSAGE_LOGIN)
public class NetworkMessageLogin extends NetworkSerializable {
    private String username;

    public NetworkMessageLogin() {} // Required for factory

    public NetworkMessageLogin(String username) {
        this.username = username;
    }

    @NetworkSerializableProperty(0)
    public String getUsername() { return username; }

    public static NetworkMessageLogin deserialize(byte[] data) throws Exception {
        NetworkMessageLogin msg = new NetworkMessageLogin();
        msg.readFrom(data);
        return msg;
    }

    @Override
    public String toString() {
        return "Login{username='" + getUsername() + "'}";
    }
}