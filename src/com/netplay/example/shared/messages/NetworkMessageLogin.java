package com.netplay.example.shared.messages;

public class NetworkMessageLogin {
    private String username;

    public NetworkMessageLogin() {}

    public NetworkMessageLogin(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "Login{username='" + username + "'}";
    }
}
