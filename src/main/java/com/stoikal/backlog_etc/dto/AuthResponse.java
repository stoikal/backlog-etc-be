package com.stoikal.backlog_etc.dto;

public class AuthResponse {
    private final String token;
    private final String email;
    private final String username;

    public AuthResponse(String token, String email, String username) {
        this.token = token;
        this.email = email;
        this.username = username;
    }

    public String getToken() { return token; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
}
