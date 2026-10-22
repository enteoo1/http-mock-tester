package com.example.mocktester.model;

import lombok.Data;

@Data
public class Scenario {
    private Long id;
    private Long contextId;
    private String name;
    private String description;
    /** Comma separated list (GET,POST,...) or the single value ANY. */
    private String allowedMethods;
    /** OPTIONAL, REQUIRED or FORBIDDEN. */
    private String bodyMode;
    private boolean requireValidJson;
    /** NONE, BASIC, BEARER, API_KEY or OAUTH2 (only presence of the header/values is checked). */
    private String authType;
    private String apiKeyHeader;
    private int responseStatus;
    private String responseBody;
    private String responseContentType;
    /** One "Name: value" per line. */
    private String responseHeaders;
    private int delayMs;
    private boolean enabled;
    /** Copy the headers of the incoming request into the response. */
    private boolean copyRequestHeaders;
    /** Append the incoming request body at the end of the response body. */
    private boolean appendRequestBody;
    private String createdAt;
}
