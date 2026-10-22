package com.example.mocktester.repo;

import com.example.mocktester.model.RequestLog;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class LogRepository extends BaseRepo {

    private static final RowMapper<RequestLog> LIST_MAPPER = (rs, i) -> {
        RequestLog l = new RequestLog();
        l.setId(rs.getLong("id"));
        l.setLogIndex(rs.getLong("log_index"));
        l.setPathUsername(rs.getString("path_username"));
        l.setContextName(rs.getString("context_name"));
        l.setScenarioName(rs.getString("scenario_name"));
        l.setReceivedAt(rs.getString("received_at"));
        l.setMethod(rs.getString("method"));
        l.setRemoteIp(rs.getString("remote_ip"));
        l.setOutcome(rs.getString("outcome"));
        l.setResponseStatus(rs.getInt("response_status"));
        long d = rs.getLong("duration_ms");
        l.setDurationMs(rs.wasNull() ? null : d);
        return l;
    };

    private static final RowMapper<RequestLog> FULL_MAPPER = (rs, i) -> {
        RequestLog l = LIST_MAPPER.mapRow(rs, i);
        l.setUserId(rs.getLong("user_id"));
        l.setRespondedAt(rs.getString("responded_at"));
        l.setUrl(rs.getString("url"));
        l.setQueryString(rs.getString("query_string"));
        l.setRequestHeaders(rs.getString("request_headers"));
        l.setRequestBody(rs.getString("request_body"));
        l.setRequestBodyTruncated(rs.getInt("request_body_truncated") == 1);
        l.setResponseHeaders(rs.getString("response_headers"));
        l.setResponseBody(rs.getString("response_body"));
        return l;
    };

    public LogRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public long insert(RequestLog l) {
        // log_index: running request number per user (the pool has a single connection, so this is race free)
        return insert("INSERT INTO request_log(log_index, user_id, context_id, scenario_id, path_username, "
                        + "context_name, scenario_name, received_at, responded_at, method, url, query_string, "
                        + "remote_ip, request_headers, request_body, request_body_truncated, outcome, "
                        + "response_status, response_headers, response_body, duration_ms) "
                        + "VALUES ((SELECT COALESCE(MAX(log_index), 0) + 1 FROM request_log WHERE user_id IS ?), "
                        + "?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                l.getUserId(), l.getUserId(), l.getContextId(), l.getScenarioId(), l.getPathUsername(), l.getContextName(),
                l.getScenarioName(), l.getReceivedAt(), l.getRespondedAt(), l.getMethod(), l.getUrl(),
                l.getQueryString(), l.getRemoteIp(), l.getRequestHeaders(), l.getRequestBody(),
                flag(l.isRequestBodyTruncated()), l.getOutcome(), l.getResponseStatus(), l.getResponseHeaders(),
                l.getResponseBody(), l.getDurationMs());
    }

    public List<RequestLog> search(long userId, String context, String scenario, String outcome,
                                   int limit, int offset) {
        StringBuilder sql = new StringBuilder("SELECT id, log_index, path_username, context_name, scenario_name, received_at, "
                + "method, remote_ip, outcome, response_status, duration_ms FROM request_log WHERE user_id = ?");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (context != null && !context.isBlank()) {
            sql.append(" AND context_name = ? COLLATE NOCASE");
            args.add(context.trim());
        }
        if (scenario != null && !scenario.isBlank()) {
            sql.append(" AND scenario_name = ? COLLATE NOCASE");
            args.add(scenario.trim());
        }
        if (outcome != null && !outcome.isBlank()) {
            sql.append(" AND outcome = ?");
            args.add(outcome.trim());
        }
        sql.append(" ORDER BY received_at DESC, id DESC LIMIT ? OFFSET ?");
        args.add(limit);
        args.add(offset);
        return jdbc.query(sql.toString(), LIST_MAPPER, args.toArray());
    }

    public Optional<RequestLog> findByIdAndUser(long id, long userId) {
        return jdbc.query("SELECT * FROM request_log WHERE id = ? AND user_id = ?", FULL_MAPPER, id, userId)
                .stream().findFirst();
    }

    public int deleteByIdAndUser(long id, long userId) {
        return jdbc.update("DELETE FROM request_log WHERE id = ? AND user_id = ?", id, userId);
    }

    public int deleteAllByUser(long userId) {
        return jdbc.update("DELETE FROM request_log WHERE user_id = ?", userId);
    }

    public int deleteOlderThan(String cutoffDbTimestamp) {
        return jdbc.update("DELETE FROM request_log WHERE received_at < ?", cutoffDbTimestamp);
    }
}
