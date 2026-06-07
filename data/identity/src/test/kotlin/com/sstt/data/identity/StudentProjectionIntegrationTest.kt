package com.sstt.data.identity

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.model.StoredEvent
import com.sstt.data.eventstore.postgres.PostgreSqlEventStore
import com.sstt.data.eventstore.postgres.PostgreSqlProjectionOffsetStore
import com.sstt.data.eventstore.projection.ReactiveProjectionRunner
import com.sstt.data.identity.events.IdentityEventTypes
import com.sstt.data.identity.model.RegisterStudentCommand
import io.r2dbc.spi.ConnectionFactories
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.test.StepVerifier

/**
 * Integration test for the student projection and read repository.
 *
 * The test is disabled by default because it needs the local PostgreSQL from
 * docker-compose.yml. Set `SSTT_IDENTITY_INTEGRATION_TESTS=true` to run it.
 */
class StudentProjectionIntegrationTest {
    @BeforeEach
    fun resetDatabase() {
        resetSchemas()
    }

    @Test
    fun `reactive runner projects newly appended student events`() {
        val eventStore = PostgreSqlEventStore(
            databaseClient = databaseClient,
            transactionalOperator = transactionalOperator,
            objectMapper = objectMapper,
            clock = fixedClock,
        )
        val projection = StudentProjection(databaseClient)
        val repository = StudentReadRepository(databaseClient)
        val runner = ReactiveProjectionRunner(
            eventStore = eventStore,
            offsetStore = PostgreSqlProjectionOffsetStore(databaseClient, fixedClock),
            projections = listOf(projection),
        )
        val studentId = uuid(10)

        StepVerifier.create(
            StudentEventWriter(eventStore, objectMapper).register(
                RegisterStudentCommand(
                    studentId = studentId,
                    eventId = uuid(11),
                    fullName = "Anna Smirnova",
                    email = "anna@example.com",
                    groupName = "IKBO-03-23",
                ),
            ),
        )
            .expectNextCount(1)
            .verifyComplete()

        StepVerifier.create(runner.runOnce(StudentProjection.PROJECTION_NAME))
            .assertNext { result ->
                check(result.processedEvents == 1L)
                check(result.lastGlobalPosition == 1L)
            }
            .verifyComplete()

        StepVerifier.create(repository.findById(studentId))
            .assertNext { student ->
                check(student.fullName == "Anna Smirnova")
                check(student.email == "anna@example.com")
                check(student.groupName == "IKBO-03-23")
            }
            .verifyComplete()
    }

    @Test
    fun `projects student events into current student read model`() {
        val projection = StudentProjection(databaseClient)
        val repository = StudentReadRepository(databaseClient)
        val studentId = uuid(1)

        StepVerifier.create(
            projection.apply(
                storedEvent(
                    studentId = studentId,
                    streamVersion = 1,
                    eventType = IdentityEventTypes.STUDENT_REGISTERED,
                    fullName = "Ivan Ivanov",
                    email = "ivan@example.com",
                    groupName = "IKBO-01-23",
                ),
            ),
        )
            .verifyComplete()

        StepVerifier.create(
            projection.apply(
                storedEvent(
                    studentId = studentId,
                    streamVersion = 2,
                    eventType = IdentityEventTypes.STUDENT_PROFILE_CHANGED,
                    fullName = "Ivan Petrov",
                    email = "petrov@example.com",
                    groupName = "IKBO-02-23",
                ),
            ),
        )
            .verifyComplete()

        StepVerifier.create(repository.findById(studentId))
            .assertNext { student ->
                check(student.fullName == "Ivan Petrov")
                check(student.email == "petrov@example.com")
                check(student.groupName == "IKBO-02-23")
                check(student.streamVersion == 2L)
            }
            .verifyComplete()
    }

    companion object {
        private val objectMapper = ObjectMapper()
        private val fixedClock = Clock.fixed(Instant.parse("2026-06-07T12:00:00Z"), ZoneOffset.UTC)
        private lateinit var databaseClient: DatabaseClient
        private lateinit var transactionalOperator: TransactionalOperator

        @JvmStatic
        @BeforeAll
        fun setup() {
            assumeTrue(System.getenv("SSTT_IDENTITY_INTEGRATION_TESTS") == "true")

            val connectionFactory = ConnectionFactories.get(
                System.getenv("SSTT_TEST_R2DBC_URL")
                    ?: "r2dbc:postgresql://sstt:sstt@localhost:5432/sstt",
            )
            databaseClient = DatabaseClient.create(connectionFactory)
            transactionalOperator = TransactionalOperator.create(R2dbcTransactionManager(connectionFactory))
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            if (
                System.getenv("SSTT_IDENTITY_INTEGRATION_TESTS") == "true" &&
                this::databaseClient.isInitialized
            ) {
                databaseClient.sql("drop schema if exists projections cascade")
                    .fetch()
                    .rowsUpdated()
                    .then(databaseClient.sql("drop schema if exists eventstore cascade").fetch().rowsUpdated())
                    .then(databaseClient.sql("drop table if exists flyway_schema_history").fetch().rowsUpdated())
                    .block()
            }
        }

        private fun resetSchemas() {
            databaseClient.sql("drop schema if exists projections cascade")
                .fetch()
                .rowsUpdated()
                .then(databaseClient.sql("drop schema if exists eventstore cascade").fetch().rowsUpdated())
                .then(databaseClient.sql("drop table if exists flyway_schema_history").fetch().rowsUpdated())
                .then(databaseClient.sql(readMigration("eventstore/V1__eventstore_schema.sql")).fetch().rowsUpdated())
                .then(databaseClient.sql(readMigration("identity/V2__identity_students_projection.sql")).fetch().rowsUpdated())
                .block()
        }

        private fun readMigration(relativePath: String): String {
            return Files.readString(Path.of("../migration/src/main/resources/db/migration/$relativePath"))
        }

        private fun storedEvent(
            studentId: UUID,
            streamVersion: Long,
            eventType: String,
            fullName: String,
            email: String,
            groupName: String,
        ): StoredEvent {
            return StoredEvent(
                eventId = UUID.randomUUID(),
                globalPosition = streamVersion,
                streamId = uuid(100),
                streamName = StudentEventWriter.streamName(studentId),
                streamType = StudentEventWriter.STREAM_TYPE,
                streamVersion = streamVersion,
                eventType = eventType,
                eventVersion = 1,
                occurredAt = Instant.parse("2026-06-07T12:00:00Z").plusSeconds(streamVersion),
                actorId = null,
                correlationId = null,
                causationId = null,
                payload = objectMapper.valueToTree(
                    mapOf(
                        "student_id" to studentId,
                        "full_name" to fullName,
                        "email" to email,
                        "group_name" to groupName,
                    ),
                ),
                metadata = objectMapper.createObjectNode(),
            )
        }

        private fun uuid(value: Long): UUID {
            return UUID.fromString("00000000-0000-0000-0000-${value.toString(16).padStart(12, '0')}")
        }
    }
}
