package com.example.mocktester.repo;

import com.example.mocktester.model.MockContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ContextRepository extends BaseRepo {

    private static final RowMapper<MockContext> MAPPER = (rs, i) -> {
        MockContext c = new MockContext();
        c.setId(rs.getLong("id"));
        c.setUserId(rs.getLong("user_id"));
        c.setName(rs.getString("name"));
        c.setDescription(rs.getString("description"));
        c.setCreatedAt(rs.getString("created_at"));
        return c;
    };

    public ContextRepository(JdbcTemplate jdbc) {
        super(jdbc);
    }

    public List<MockContext> findByUser(long userId) {
        return jdbc.query("SELECT * FROM mock_context WHERE user_id = ? ORDER BY name", MAPPER, userId);
    }

    public Optional<MockContext> findByIdAndUser(long id, long userId) {
        return jdbc.query("SELECT * FROM mock_context WHERE id = ? AND user_id = ?", MAPPER, id, userId)
                .stream().findFirst();
    }

    public Optional<MockContext> findByUserAndName(long userId, String name) {
        return jdbc.query("SELECT * FROM mock_context WHERE user_id = ? AND name = ?", MAPPER, userId, name)
                .stream().findFirst();
    }

    public long insert(long userId, String name, String description, String createdAt) {
        return insert("INSERT INTO mock_context(user_id, name, description, created_at) VALUES (?,?,?,?)",
                userId, name, description, createdAt);
    }

    public void delete(long id, long userId) {
        jdbc.update("DELETE FROM mock_context WHERE id = ? AND user_id = ?", id, userId);
    }
}
