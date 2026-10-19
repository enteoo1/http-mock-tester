package com.example.mocktester.repo;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

abstract class BaseRepo {

    protected final JdbcTemplate jdbc;

    protected BaseRepo(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Runs an INSERT and returns the generated id (same connection for both statements). */
    protected long insert(String sql, Object... args) {
        return jdbc.execute((ConnectionCallback<Long>) con -> {
            try (PreparedStatement ps = con.prepareStatement(sql)) {
                for (int i = 0; i < args.length; i++) {
                    ps.setObject(i + 1, args[i]);
                }
                ps.executeUpdate();
            }
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT last_insert_rowid()")) {
                rs.next();
                return rs.getLong(1);
            }
        });
    }

    protected static int flag(boolean b) {
        return b ? 1 : 0;
    }
}
