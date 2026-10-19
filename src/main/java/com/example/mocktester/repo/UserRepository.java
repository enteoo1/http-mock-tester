package com.example.mocktester.repo;

import com.example.mocktester.model.AppUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class UserRepository extends BaseRepo {

    private static final RowMapper<AppUser> MAPPER = (rs, i) -> {
        AppUser u = new AppUser();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setTimezone(rs.getString("timezone"));
        u.setMustChangePassword(rs.getInt("must_change_password") == 1);
        u.setCreatedAt(rs.getString("created_at"));
        return u;
    };

    public UserRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public Optional<AppUser> findByUsername(String username) {
        return jdbc.query("SELECT * FROM app_user WHERE username = ?", MAPPER, username).stream().findFirst();
    }

    public List<AppUser> findAll() {
        return jdbc.query("SELECT * FROM app_user ORDER BY username", MAPPER);
    }

    public long count() {
        Long c = jdbc.queryForObject("SELECT COUNT(*) FROM app_user", Long.class);
        return c == null ? 0 : c;
    }

    public long insert(String username, String passwordHash, String role, String timezone,
                       boolean mustChange, String createdAt) {
        return insert("INSERT INTO app_user(username, password_hash, role, timezone, must_change_password, created_at) "
                + "VALUES (?,?,?,?,?,?)", username, passwordHash, role, timezone, flag(mustChange), createdAt);
    }

    public void updatePassword(long id, String passwordHash) {
        jdbc.update("UPDATE app_user SET password_hash = ?, must_change_password = 0 WHERE id = ?", passwordHash, id);
    }

    public void updateTimezone(long id, String timezone) {
        jdbc.update("UPDATE app_user SET timezone = ? WHERE id = ?", timezone, id);
    }
}
