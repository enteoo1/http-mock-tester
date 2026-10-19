package com.example.mocktester.model;

import lombok.Data;

@Data
public class RequestLog {
    private Long id;
    private Long userId;
    private Long contextId;
    private Long scenarioId;
    private String pathUsername;
    private String contextName;
    private String scenarioName;
    private String receivedAt;
    private String respondedAt;
    private String method;
    private String url;
    private String queryString;
    private String remoteIp;
    private String requestHeaders;
    private String requestBody;
    private boolean requestBodyTruncated;
    private String outcome;
    private int responseStatus;
    private String responseHeaders;
    private String responseBody;
    private Long durationMs;
}
