package com.example.mocktester.mock;

import com.example.mocktester.model.AppUser;
import com.example.mocktester.model.MockContext;
import com.example.mocktester.model.RequestLog;
import com.example.mocktester.model.Scenario;
import com.example.mocktester.repo.ContextRepository;
import com.example.mocktester.repo.LogRepository;
import com.example.mocktester.repo.ScenarioRepository;
import com.example.mocktester.repo.UserRepository;
import com.example.mocktester.service.TimeUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Service
public class MockService {

    private static final Logger log = LoggerFactory.getLogger(MockService.class);
    private static final Pattern HEADER_NAME = Pattern.compile("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$");

    private final UserRepository users;
    private final ContextRepository contexts;
    private final ScenarioRepository scenarios;
    private final LogRepository logs;
    private final ObjectMapper mapper;
    private final int maxBody;

    public MockService(UserRepository users, ContextRepository contexts, ScenarioRepository scenarios,
                       LogRepository logs, ObjectMapper mapper,
                       @Value("${app.max-logged-body-bytes:10485760}") int maxBody) {
        this.users = users;
        this.contexts = contexts;
        this.scenarios = scenarios;
        this.logs = logs;
        this.mapper = mapper;
        this.maxBody = maxBody;
    }

    public MockResponse process(String username, String contextName, String scenarioName,
                                HttpServletRequest req) throws IOException {
        Instant received = Instant.now();
        long t0 = System.nanoTime();

        byte[] raw = req.getInputStream().readNBytes(maxBody + 1);
        boolean truncated = raw.length > maxBody;
        byte[] body = truncated ? Arrays.copyOf(raw, maxBody) : raw;

        Optional<AppUser> user = users.findByUsername(username);
        Optional<MockContext> ctx = user.flatMap(u -> contexts.findByUserAndName(u.getId(), contextName));
        Optional<Scenario> sc = ctx.flatMap(c -> scenarios.findByContextAndName(c.getId(), scenarioName));

        String outcome;
        MockResponse response;

        if (sc.isEmpty()) {
            outcome = "NOT_FOUND";
            response = error(404, "No scenario found for this user/context/scenario");
        } else {
            Scenario s = sc.get();
            String method = req.getMethod().toUpperCase(Locale.ROOT);
            boolean hasBody = body.length > 0;
            if (!s.isEnabled()) {
                outcome = "DISABLED";
                response = error(503, "Scenario is disabled");
            } else if (!methodAllowed(s, method)) {
                outcome = "METHOD_NOT_ALLOWED";
                response = error(405, "Method " + method + " is not allowed for this scenario");
                response.headers().put("Allow", s.getAllowedMethods().replace(",", ", "));
            } else if (!authOk(s, req)) {
                outcome = "UNAUTHORIZED";
                response = error(401, "Missing or empty credentials for auth type " + s.getAuthType());
                if ("BASIC".equals(s.getAuthType())) {
                    response.headers().put("WWW-Authenticate", "Basic realm=\"mock\"");
                } else if ("BEARER".equals(s.getAuthType()) || "OAUTH2".equals(s.getAuthType())) {
                    response.headers().put("WWW-Authenticate", "Bearer");
                }
            } else if ("FORBIDDEN".equals(s.getBodyMode()) && hasBody) {
                outcome = "BAD_REQUEST";
                response = error(400, "This scenario does not accept a request body");
            } else if ("REQUIRED".equals(s.getBodyMode()) && !hasBody) {
                outcome = "BAD_REQUEST";
                response = error(400, "This scenario requires a request body");
            } else if (s.isRequireValidJson() && hasBody && !validJson(body)) {
                outcome = "BAD_REQUEST";
                response = error(400, "Request body is not valid JSON");
            } else {
                outcome = "MATCHED";
                if (s.getDelayMs() > 0) {
                    try {
                        Thread.sleep(s.getDelayMs());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                response = configured(s);
            }
        }

        Instant responded = Instant.now();
        saveLog(username, contextName, scenarioName, user, ctx, sc, req, body, truncated, received, responded,
                (System.nanoTime() - t0) / 1_000_000, outcome, response);
        return response;
    }

    // ---------------------------------------------------------------- helpers

    private boolean methodAllowed(Scenario s, String method) {
        String allowed = s.getAllowedMethods();
        if (allowed == null || allowed.isBlank() || allowed.equalsIgnoreCase("ANY")) {
            return true;
        }
        for (String m : allowed.split(",")) {
            if (m.trim().equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    /** Only checks that the credentials/headers are present, never their actual values. */
    private boolean authOk(Scenario s, HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        String type = s.getAuthType() == null ? "NONE" : s.getAuthType();
        switch (type) {
            case "BASIC":
                if (auth == null || !auth.regionMatches(true, 0, "Basic ", 0, 6)) {
                    return false;
                }
                try {
                    String decoded = new String(Base64.getDecoder().decode(auth.substring(6).trim()),
                            StandardCharsets.UTF_8);
                    int i = decoded.indexOf(':');
                    return i > 0 && i < decoded.length() - 1;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            case "BEARER":
            case "OAUTH2":
                return auth != null && auth.regionMatches(true, 0, "Bearer ", 0, 7)
                        && !auth.substring(7).trim().isEmpty();
            case "API_KEY":
                String header = (s.getApiKeyHeader() == null || s.getApiKeyHeader().isBlank())
                        ? "X-API-Key" : s.getApiKeyHeader();
                String value = req.getHeader(header);
                return value != null && !value.isBlank();
            default:
                return true;
        }
    }

    private boolean validJson(byte[] body) {
        try {
            mapper.readTree(body);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private MockResponse error(int status, String message) {
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        byte[] body;
        try {
            body = mapper.writeValueAsBytes(Map.of("error", message));
        } catch (IOException e) {
            body = ("{\"error\":\"" + status + "\"}").getBytes(StandardCharsets.UTF_8);
        }
        return new MockResponse(status, headers, body);
    }

    private MockResponse configured(Scenario s) {
        Map<String, String> headers = new LinkedHashMap<>();
        if (s.getResponseHeaders() != null) {
            for (String line : s.getResponseHeaders().split("\\R")) {
                int i = line.indexOf(':');
                if (i > 0) {
                    String name = line.substring(0, i).trim();
                    String value = line.substring(i + 1).trim();
                    if (HEADER_NAME.matcher(name).matches()) {
                        headers.put(name, value);
                    }
                }
            }
        }
        byte[] body = null;
        if (s.getResponseBody() != null && !s.getResponseBody().isEmpty()) {
            body = s.getResponseBody().getBytes(StandardCharsets.UTF_8);
            boolean hasContentType = headers.keySet().stream().anyMatch(k -> k.equalsIgnoreCase("Content-Type"));
            if (!hasContentType) {
                String ct = (s.getResponseContentType() == null || s.getResponseContentType().isBlank())
                        ? "application/json" : s.getResponseContentType().trim();
                headers.put("Content-Type", ct);
            }
        }
        return new MockResponse(s.getResponseStatus(), headers, body);
    }

    private void saveLog(String username, String contextName, String scenarioName, Optional<AppUser> user,
                         Optional<MockContext> ctx, Optional<Scenario> sc, HttpServletRequest req, byte[] body,
                         boolean truncated, Instant received, Instant responded, long durationMs,
                         String outcome, MockResponse response) {
        try {
            RequestLog l = new RequestLog();
            l.setUserId(user.map(AppUser::getId).orElse(null));
            l.setContextId(ctx.map(MockContext::getId).orElse(null));
            l.setScenarioId(sc.map(Scenario::getId).orElse(null));
            l.setPathUsername(username);
            l.setContextName(contextName);
            l.setScenarioName(scenarioName);
            l.setReceivedAt(TimeUtil.toDb(received));
            l.setRespondedAt(TimeUtil.toDb(responded));
            l.setMethod(req.getMethod());
            l.setUrl(req.getRequestURL().toString());
            l.setQueryString(req.getQueryString());
            l.setRemoteIp(req.getRemoteAddr());
            l.setRequestHeaders(headersToString(req));
            l.setRequestBody(new String(body, StandardCharsets.UTF_8));
            l.setRequestBodyTruncated(truncated);
            l.setOutcome(outcome);
            l.setResponseStatus(response.status());
            StringBuilder rh = new StringBuilder();
            response.headers().forEach((k, v) -> rh.append(k).append(": ").append(v).append('\n'));
            l.setResponseHeaders(rh.toString());
            l.setResponseBody(response.body() == null ? null : new String(response.body(), StandardCharsets.UTF_8));
            l.setDurationMs(durationMs);
            logs.insert(l);
        } catch (RuntimeException e) {
            log.error("Failed to store request log", e);
        }
    }

    private String headersToString(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        Enumeration<String> names = req.getHeaderNames();
        while (names != null && names.hasMoreElements()) {
            String name = names.nextElement();
            Enumeration<String> values = req.getHeaders(name);
            while (values.hasMoreElements()) {
                sb.append(name).append(": ").append(values.nextElement()).append('\n');
            }
        }
        return sb.toString();
    }
}
