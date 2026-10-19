package com.example.mocktester.repo;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class SettingsRepository extends BaseRepo {

    public SettingsRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public Optional<String> get(String key) {
        return jdbc.query("SELECT setting_value FROM app_setting WHERE setting_key = ?",
                (rs, i) -> rs.getString(1), key).stream().findFirst();
    }

    public void set(String key, String value) {
        jdbc.update("INSERT INTO app_setting(setting_key, setting_value) VALUES (?,?) "
                + "ON CONFLICT(setting_key) DO UPDATE SET setting_value = excluded.setting_value", key, value);
    }
}
