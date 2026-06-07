package com.sstt.data.materials

import com.sstt.data.materials.model.MaterialRecord
import java.time.Instant
import java.util.UUID
import org.springframework.r2dbc.core.DatabaseClient
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class MaterialReadRepository(private val databaseClient: DatabaseClient) {
    fun findById(materialId: UUID): Mono<MaterialRecord> {
        return selectBase("where material_id = :material_id")
            .bind("material_id", materialId)
            .map { row -> mapRecord(row) }
            .one()
    }

    fun listByStudent(studentId: UUID): Flux<MaterialRecord> {
        return selectBase("where student_id = :student_id order by created_at desc")
            .bind("student_id", studentId)
            .map { row -> mapRecord(row) }
            .all()
    }

    private fun selectBase(whereClause: String): DatabaseClient.GenericExecuteSpec {
        return databaseClient.sql(
            """
            select material_id, student_id, file_name, content_type, storage_key, summary,
                   stream_version, created_at, updated_at
            from projections.materials
            $whereClause
            """.trimIndent(),
        )
    }

    private fun mapRecord(row: io.r2dbc.spi.Readable): MaterialRecord {
        return MaterialRecord(
            materialId = row.require("material_id", UUID::class.java),
            studentId = row.require("student_id", UUID::class.java),
            fileName = row.require("file_name", String::class.java),
            contentType = row.get("content_type", String::class.java),
            storageKey = row.require("storage_key", String::class.java),
            summary = row.get("summary", String::class.java),
            streamVersion = row.require("stream_version", java.lang.Long::class.java).toLong(),
            createdAt = row.require("created_at", Instant::class.java),
            updatedAt = row.require("updated_at", Instant::class.java),
        )
    }

    private fun <T : Any> io.r2dbc.spi.Readable.require(column: String, type: Class<T>): T {
        return get(column, type) ?: error("Column '$column' must not be null")
    }
}
