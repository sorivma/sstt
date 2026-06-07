package com.sstt.tasks;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class TaskRepository {
    private final JdbcTemplate jdbc;

    public TaskRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID create(UUID userId, TaskForm form) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into tasks
                    (id, user_id, subject_id, teacher_id, source_message_id, title, description, type,
                     status, priority, deadline_date, confidence, created_at, updated_at)
                values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, current_timestamp, current_timestamp)
                """, id, userId, form.getSubjectId(), form.getTeacherId(), form.getSourceMessageId(),
                form.getTitle(), form.getDescription(), form.getType().name(), form.getStatus().name(),
                form.getPriority().name(), toSqlDate(form.getDeadlineDate()), form.getConfidence());
        return id;
    }

    public List<StudentTask> findAllByUserId(UUID userId) {
        return jdbc.query("""
                select t.id, t.user_id, t.subject_id, s.name as subject_name, t.teacher_id,
                       te.full_name as teacher_name, t.source_message_id, t.title, t.description,
                       t.type, t.status, t.priority, t.deadline_date, t.confidence, t.created_at, t.updated_at
                from tasks t
                left join subjects s on s.id = t.subject_id
                left join teachers te on te.id = t.teacher_id
                where t.user_id = ?
                order by
                    case when t.deadline_date is null then 1 else 0 end,
                    t.deadline_date asc,
                    t.created_at desc
                """, (rs, rowNum) -> mapTask(rs), userId);
    }

    private static StudentTask mapTask(ResultSet rs) throws SQLException {
        Timestamp createdAt = rs.getTimestamp("created_at");
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        Date deadlineDate = rs.getDate("deadline_date");
        return new StudentTask(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                rs.getObject("subject_id", UUID.class),
                rs.getString("subject_name"),
                rs.getObject("teacher_id", UUID.class),
                rs.getString("teacher_name"),
                rs.getObject("source_message_id", UUID.class),
                rs.getString("title"),
                rs.getString("description"),
                TaskType.valueOf(rs.getString("type")),
                TaskStatus.valueOf(rs.getString("status")),
                TaskPriority.valueOf(rs.getString("priority")),
                deadlineDate == null ? null : deadlineDate.toLocalDate(),
                (Double) rs.getObject("confidence"),
                createdAt.toInstant(),
                updatedAt.toInstant()
        );
    }

    private static Date toSqlDate(LocalDate date) {
        return date == null ? null : Date.valueOf(date);
    }
}
