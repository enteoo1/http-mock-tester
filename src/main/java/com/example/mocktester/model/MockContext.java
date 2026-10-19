package com.example.mocktester.model;

import lombok.Data;

@Data
public class MockContext {
    private Long id;
    private Long userId;
    private String name;
    private String description;
    private String createdAt;
}
