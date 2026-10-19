package com.example.mocktester.model;

import lombok.Data;

@Data
public class AppUser {
    private Long id;
    private String username;
    private String passwordHash;
    private String role;
    private String timezone;
    private boolean mustChangePassword;
    private String createdAt;
}
