package com.example.mocktester.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

/** Adds columns introduced after the first release to databases created by an older schema.sql. */
@Component
public class SchemaMigration {

    private static final Logger log = LoggerFactory.getLogger(SchemaMigration.class);

    private final JdbcTemplate jdbc;

    public SchemaMigration(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @PostConstruct
    public void migrate() {
        addColumn("scenario", "copy_request_headers", "INTEGER NOT NULL DEFAULT 0");
        addColumn("scenario", "append_request_body", "INTEGER NOT NULL DEFAULT 0");
        if (addColumn("request_log", "log_index", "INTEGER")) {
            // number the existing entries per user in the order they were received
            jdbc.update("UPDATE request_log SET log_index = (SELECT COUNT(*) FROM request_log o "
                    + "WHERE o.user_id IS request_log.user_id AND o.id <= request_log.id)");
        }
    }

    private boolean addColumn(String table, String column, String definition) {
        List<String> existing = jdbc.query("PRAGMA table_info(" + table + ")", (rs, i) -> rs.getString("name"));
        if (existing.contains(column)) {
            return false;
        }
        jdbc.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
        log.info("Migrated schema: added {}.{}", table, column);
        return true;
    }
}
