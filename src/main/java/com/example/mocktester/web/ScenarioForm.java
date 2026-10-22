package com.example.mocktester.web;

import com.example.mocktester.model.Scenario;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Data
public class ScenarioForm {
    private String name = "";
    private String description = "";
    private List<String> methods = new ArrayList<>(List.of("POST"));
    private String bodyMode = "OPTIONAL";
    private boolean requireValidJson;
    private String authType = "NONE";
    private String apiKeyHeader = "X-API-Key";
    private Integer responseStatus = 200;
    private String responseBody = "";
    private String responseContentType = "application/json";
    private String responseHeaders = "";
    private Integer delayMs = 0;
    private boolean enabled = true;
    private boolean copyRequestHeaders;
    private boolean appendRequestBody;

    public static ScenarioForm from(Scenario s) {
        ScenarioForm f = new ScenarioForm();
        f.name = s.getName();
        f.description = s.getDescription() == null ? "" : s.getDescription();
        f.methods = new ArrayList<>(Arrays.asList(s.getAllowedMethods().split(",")));
        f.bodyMode = s.getBodyMode();
        f.requireValidJson = s.isRequireValidJson();
        f.authType = s.getAuthType();
        f.apiKeyHeader = s.getApiKeyHeader() == null ? "X-API-Key" : s.getApiKeyHeader();
        f.responseStatus = s.getResponseStatus();
        f.responseBody = s.getResponseBody() == null ? "" : s.getResponseBody();
        f.responseContentType = s.getResponseContentType() == null ? "" : s.getResponseContentType();
        f.responseHeaders = s.getResponseHeaders() == null ? "" : s.getResponseHeaders();
        f.delayMs = s.getDelayMs();
        f.enabled = s.isEnabled();
        f.copyRequestHeaders = s.isCopyRequestHeaders();
        f.appendRequestBody = s.isAppendRequestBody();
        return f;
    }

    public void applyTo(Scenario s) {
        s.setName(name.trim());
        s.setDescription(description == null ? "" : description.trim());
        s.setAllowedMethods(methods.contains("ANY") ? "ANY" : String.join(",", methods));
        s.setBodyMode(bodyMode);
        s.setRequireValidJson(requireValidJson);
        s.setAuthType(authType);
        s.setApiKeyHeader(apiKeyHeader == null ? "" : apiKeyHeader.trim());
        s.setResponseStatus(responseStatus);
        s.setResponseBody(responseBody == null ? "" : responseBody);
        s.setResponseContentType(responseContentType == null ? "" : responseContentType.trim());
        s.setResponseHeaders(responseHeaders == null ? "" : responseHeaders);
        s.setDelayMs(delayMs);
        s.setEnabled(enabled);
        s.setCopyRequestHeaders(copyRequestHeaders);
        s.setAppendRequestBody(appendRequestBody);
    }
}
