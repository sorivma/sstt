package com.sstt.academics;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SubjectRepository {
    private final JdbcTemplate jdbc;

    public SubjectRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Subject> findAllByUserId(UUID userId) {
        return jdbc.query("""
                select id, user_id, name, description, semester, created_at
                from subjects
                where user_id = ?
                order by name
                """, (rs, rowNum) -> mapSubject(rs), userId);
    }

    public Optional<Subject> findByIdAndUserId(UUID id, UUID userId) {
        return jdbc.query("""
                select id, user_id, name, description, semester, created_at
                from subjects
                where id = ? and user_id = ?
                """, (rs, rowNum) -> mapSubject(rs), id, userId).stream().findFirst();
    }

    public void create(UUID userId, SubjectForm form) {
        jdbc.update("""
                insert into subjects (id, user_id, name, description, semester, created_at)
                values (?, ?, ?, ?, ?, current_timestamp)
                """, UUID.randomUUID(), userId, form.getName(), form.getDescription(), form.getSemester());
    }

    private static Subject mapSubject(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new Subject(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("semester"),
                createdAt.toInstant()
        );
    }
}
