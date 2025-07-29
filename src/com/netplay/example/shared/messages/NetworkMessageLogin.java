package com.netplay.example.shared.messages;

import com.netplay.shared.NetworkSerializable;
import com.netplay.shared.NetworkSerializableProperty;

public class NetworkMessageLogin implements NetworkSerializable {
    private String username;

    public NetworkMessageLogin() {} // Required for factory

    public NetworkMessageLogin(String username) {
        this.username = username;
    }

    @NetworkSerializableProperty(0)
    public String getUsername() { return username; }


    @Override
    public String toString() {
        return "Login{username='" + getUsername() + "'}";
    }
}