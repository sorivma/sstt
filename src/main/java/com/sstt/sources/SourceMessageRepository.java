package com.sstt.sources;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SourceMessageRepository {
    private final JdbcTemplate jdbc;

    public SourceMessageRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public UUID create(UUID userId, SourceMessageForm form) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                insert into source_messages
                    (id, user_id, source_type, raw_text, sender, received_at, processing_status, created_at)
                values (?, ?, ?, ?, ?, current_timestamp, ?, current_timestamp)
                """, id, userId, form.getSourceType().name(), form.getRawText(), form.getSender(),
                ProcessingStatus.NEW.name());
        return id;
    }

    public Optional<SourceMessage> findByIdAndUserId(UUID id, UUID userId) {
        return jdbc.query("""
                select id, user_id, source_type, raw_text, sender, received_at, processing_status, created_at
                from source_messages
                where id = ? and user_id = ?
                """, (rs, rowNum) -> mapSourceMessage(rs), id, userId).stream().findFirst();
    }

    public List<SourceMessage> findRecentByUserId(UUID userId) {
        return jdbc.query("""
                select id, user_id, source_type, raw_text, sender, received_at, processing_status, created_at
                from source_messages
                where user_id = ?
                order by created_at desc
                limit 20
                """, (rs, rowNum) -> mapSourceMessage(rs), userId);
    }

    public void markProcessed(UUID id, UUID userId) {
        jdbc.update("""
                update source_messages
                set processing_status = ?
                where id = ? and user_id = ?
                """, ProcessingStatus.PROCESSED.name(), id, userId);
    }

    private static SourceMessage mapSourceMessage(ResultSet rs) throws SQLException {
        Timestamp receivedAt = rs.getTimestamp("received_at");
        Timestamp createdAt = rs.getTimestamp("created_at");
        return new SourceMessage(
                rs.getObject("id", UUID.class),
                rs.getObject("user_id", UUID.class),
                SourceType.valueOf(rs.getString("source_type")),
                rs.getString("raw_text"),
                rs.getString("sender"),
                receivedAt.toInstant(),
                ProcessingStatus.valueOf(rs.getString("processing_status")),
                createdAt.toInstant()
        );
    }
}
