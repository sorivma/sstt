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
public class TeacherRepository {
    private final JdbcTemplate jdbc;

    public TeacherRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Teacher> findAllByUserId(UUID userId) {
        return jdbc.query("""
                select id, user_id, full_name, email, telegram, notes, created_at
                from teachers
                where user_id = ?
                order by full_name
                """, (rs, rowNum) -> mapTeacher(rs), userId);
    }

    public Optional<Teacher> findByIdAndUserId(UUID id, UUID userId) {
        return jdbc.query("""
                select id, user_id, full_name, email, telegram, notes, created_at
                from teachers
                where id = ? and user_id = ?
                """, (rs, rowNum) -> mapTeacher(rs), id, userId).stream().findFirst();
    }

    public void create(UUID userId, TeacherForm form) {
        jdbc.update("""
                insert into teachers (id, user_id, full_name, email, telegram, notes, created_at)
                values (?, ?, ?, ?, ?, ?, current_timestamp)
                """, UUID.randomUUID(), userId, form.getFullName(), form.getEmail(), form.getTelegram(), form.getNotes());
    }

    private static Teacher mapTeacher(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new Teacher(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getString("full_name"),
                rs.getString("email"),
                rs.getString("telegram"),
                rs.getString("notes"),
                createdAt.toInstant()
        );
    }
}
