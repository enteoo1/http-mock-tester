package com.example.mocktester.mock;

import java.util.Map;

public record MockResponse(int status, Map<String, String> headers, byte[] body) {
}
