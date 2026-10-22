package com.example.mocktester.repo;

import com.example.mocktester.model.Scenario;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ScenarioRepository extends BaseRepo {

    private static final RowMapper<Scenario> MAPPER = (rs, i) -> {
        Scenario s = new Scenario();
        s.setId(rs.getLong("id"));
        s.setContextId(rs.getLong("context_id"));
        s.setName(rs.getString("name"));
        s.setDescription(rs.getString("description"));
        s.setAllowedMethods(rs.getString("allowed_methods"));
        s.setBodyMode(rs.getString("body_mode"));
        s.setRequireValidJson(rs.getInt("require_valid_json") == 1);
        s.setAuthType(rs.getString("auth_type"));
        s.setApiKeyHeader(rs.getString("api_key_header"));
        s.setResponseStatus(rs.getInt("response_status"));
        s.setResponseBody(rs.getString("response_body"));
        s.setResponseContentType(rs.getString("response_content_type"));
        s.setResponseHeaders(rs.getString("response_headers"));
        s.setDelayMs(rs.getInt("delay_ms"));
        s.setEnabled(rs.getInt("enabled") == 1);
        s.setCopyRequestHeaders(rs.getInt("copy_request_headers") == 1);
        s.setAppendRequestBody(rs.getInt("append_request_body") == 1);
        s.setCreatedAt(rs.getString("created_at"));
        return s;
    };

    public ScenarioRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public List<Scenario> findByContext(long contextId) {
        return jdbc.query("SELECT * FROM scenario WHERE context_id = ? ORDER BY name", MAPPER, contextId);
    }

    public Optional<Scenario> findByContextAndName(long contextId, String name) {
        return jdbc.query("SELECT * FROM scenario WHERE context_id = ? AND name = ?", MAPPER, contextId, name)
                .stream().findFirst();
    }

    /** Only returns the scenario if its context belongs to the given user. */
    public Optional<Scenario> findByIdAndUser(long id, long userId) {
        return jdbc.query("SELECT s.* FROM scenario s JOIN mock_context c ON c.id = s.context_id "
                + "WHERE s.id = ? AND c.user_id = ?", MAPPER, id, userId).stream().findFirst();
    }

    public long insert(Scenario s) {
        return insert("INSERT INTO scenario(context_id, name, description, allowed_methods, body_mode, "
                        + "require_valid_json, auth_type, api_key_header, response_status, response_body, "
                        + "response_content_type, response_headers, delay_ms, enabled, copy_request_headers, "
                        + "append_request_body, created_at) "
                        + "VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                s.getContextId(), s.getName(), s.getDescription(), s.getAllowedMethods(), s.getBodyMode(),
                flag(s.isRequireValidJson()), s.getAuthType(), s.getApiKeyHeader(), s.getResponseStatus(),
                s.getResponseBody(), s.getResponseContentType(), s.getResponseHeaders(), s.getDelayMs(),
                flag(s.isEnabled()), flag(s.isCopyRequestHeaders()), flag(s.isAppendRequestBody()),
                s.getCreatedAt());
    }

    public void update(Scenario s) {
        jdbc.update("UPDATE scenario SET name = ?, description = ?, allowed_methods = ?, body_mode = ?, "
                        + "require_valid_json = ?, auth_type = ?, api_key_header = ?, response_status = ?, "
                        + "response_body = ?, response_content_type = ?, response_headers = ?, delay_ms = ?, "
                        + "enabled = ?, copy_request_headers = ?, append_request_body = ? WHERE id = ?",
                s.getName(), s.getDescription(), s.getAllowedMethods(), s.getBodyMode(),
                flag(s.isRequireValidJson()), s.getAuthType(), s.getApiKeyHeader(), s.getResponseStatus(),
                s.getResponseBody(), s.getResponseContentType(), s.getResponseHeaders(), s.getDelayMs(),
                flag(s.isEnabled()), flag(s.isCopyRequestHeaders()), flag(s.isAppendRequestBody()), s.getId());
    }

    public void setEnabled(long id, boolean enabled) {
        jdbc.update("UPDATE scenario SET enabled = ? WHERE id = ?", flag(enabled), id);
    }

    public void delete(long id) {
        jdbc.update("DELETE FROM scenario WHERE id = ?", id);
    }
}
