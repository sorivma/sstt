package com.sstt.data.eventstore.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.error.StreamConcurrencyException
import com.sstt.data.eventstore.model.AppendEventsCommand
import com.sstt.data.eventstore.model.ExpectedVersion
import com.sstt.data.eventstore.model.NewEvent
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
 * Integration checks for the PostgreSQL/R2DBC event store adapter.
 *
 * The test is disabled by default because it needs a running local PostgreSQL
 * matching docker-compose.yml. Set `SSTT_EVENTSTORE_INTEGRATION_TESTS=true` to
 * run it during storage work.
 */
class PostgreSqlEventStoreIntegrationTest {
    @BeforeEach
    fun resetDatabase() {
        resetEventstoreSchema()
    }

    @Test
    fun `appends and reads generic json events`() {
        val eventStore = PostgreSqlEventStore(
            databaseClient = databaseClient,
            transactionalOperator = transactionalOperator,
            objectMapper = objectMapper,
            clock = fixedClock,
            streamIdGenerator = deterministicUuidGenerator(start = 20),
        )

        val command = AppendEventsCommand(
            streamName = "student:00000000-0000-0000-0000-000000000001",
            streamType = "student",
            expectedVersion = ExpectedVersion.NoStream,
            events = listOf(
                NewEvent(
                    eventId = uuid(1),
                    eventType = "StudentRegistered",
                    eventVersion = 1,
                    payload = objectMapper.readTree(
                        """
                        {
                          "student_id": "00000000-0000-0000-0000-000000000001",
                          "full_name": "Test Student"
                        }
                        """.trimIndent(),
                    ),
                    metadata = objectMapper.createObjectNode(),
                ),
            ),
        )

        StepVerifier.create(eventStore.append(command))
            .assertNext { result ->
                check(result.previousStreamVersion == 0L)
                check(result.currentStreamVersion == 1L)
                check(result.events.single().streamVersion == 1L)
                check(result.events.single().payload["full_name"].asText() == "Test Student")
            }
            .verifyComplete()

        StepVerifier.create(eventStore.loadStream(command.streamName))
            .assertNext { event ->
                check(event.eventType == "StudentRegistered")
                check(event.globalPosition > 0)
                check(event.occurredAt == Instant.parse("2026-06-07T12:00:00Z"))
            }
            .verifyComplete()
    }

    @Test
    fun `rejects append when expected stream version is stale`() {
        val eventStore = PostgreSqlEventStore(
            databaseClient = databaseClient,
            transactionalOperator = transactionalOperator,
            objectMapper = objectMapper,
            clock = fixedClock,
            streamIdGenerator = deterministicUuidGenerator(start = 40),
        )
        val streamName = "task:00000000-0000-0000-0000-000000000001"

        StepVerifier.create(
            eventStore.append(
                AppendEventsCommand(
                    streamName = streamName,
                    streamType = "task",
                    expectedVersion = ExpectedVersion.NoStream,
                    events = listOf(testEvent("TaskCreated")),
                ),
            ),
        )
            .expectNextCount(1)
            .verifyComplete()

        StepVerifier.create(
            eventStore.append(
                AppendEventsCommand(
                    streamName = streamName,
                    streamType = "task",
                    expectedVersion = ExpectedVersion.Exact(0),
                    events = listOf(testEvent("TaskDetailsChanged")),
                ),
            ),
        )
            .expectError(StreamConcurrencyException::class.java)
            .verify()
    }

    @Test
    fun `supports no stream exact and any expected versions`() {
        val eventStore = PostgreSqlEventStore(
            databaseClient = databaseClient,
            transactionalOperator = transactionalOperator,
            objectMapper = objectMapper,
            clock = fixedClock,
            streamIdGenerator = deterministicUuidGenerator(start = 45),
        )
        val streamName = "material:00000000-0000-0000-0000-000000000001"

        StepVerifier.create(
            eventStore.append(
                AppendEventsCommand(
                    streamName = streamName,
                    streamType = "material",
                    expectedVersion = ExpectedVersion.NoStream,
                    events = listOf(testEvent("MaterialUploaded", eventId = uuid(301))),
                ),
            ),
        )
            .assertNext { result -> check(result.currentStreamVersion == 1L) }
            .verifyComplete()

        StepVerifier.create(
            eventStore.append(
                AppendEventsCommand(
                    streamName = streamName,
                    streamType = "material",
                    expectedVersion = ExpectedVersion.Exact(1),
                    events = listOf(testEvent("MaterialMetadataChanged", eventId = uuid(302))),
                ),
            ),
        )
            .assertNext { result -> check(result.currentStreamVersion == 2L) }
            .verifyComplete()

        StepVerifier.create(
            eventStore.append(
                AppendEventsCommand(
                    streamName = streamName,
                    streamType = "material",
                    expectedVersion = ExpectedVersion.Any,
                    events = listOf(testEvent("MaterialArchived", eventId = uuid(303))),
                ),
            ),
        )
            .assertNext { result -> check(result.currentStreamVersion == 3L) }
            .verifyComplete()
    }

    @Test
    fun `rejects no stream append when stream already exists`() {
        val eventStore = PostgreSqlEventStore(
            databaseClient = databaseClient,
            transactionalOperator = transactionalOperator,
            objectMapper = objectMapper,
            clock = fixedClock,
            streamIdGenerator = deterministicUuidGenerator(start = 46),
        )
        val streamName = "teacher:00000000-0000-0000-0000-000000000001"

        StepVerifier.create(
            eventStore.append(
                AppendEventsCommand(
                    streamName = streamName,
                    streamType = "teacher",
                    expectedVersion = ExpectedVersion.NoStream,
                    events = listOf(testEvent("TeacherCreated", eventId = uuid(401))),
                ),
            ),
        )
            .expectNextCount(1)
            .verifyComplete()

        StepVerifier.create(
            eventStore.append(
                AppendEventsCommand(
                    streamName = streamName,
                    streamType = "teacher",
                    expectedVersion = ExpectedVersion.NoStream,
                    events = listOf(testEvent("TeacherProfileChanged", eventId = uuid(402))),
                ),
            ),
        )
            .expectError(StreamConcurrencyException::class.java)
            .verify()
    }

    @Test
    fun `returns existing events when append is retried with the same event ids`() {
        val eventStore = PostgreSqlEventStore(
            databaseClient = databaseClient,
            transactionalOperator = transactionalOperator,
            objectMapper = objectMapper,
            clock = fixedClock,
            streamIdGenerator = deterministicUuidGenerator(start = 50),
        )
        val command = AppendEventsCommand(
            streamName = "source-message:00000000-0000-0000-0000-000000000001",
            streamType = "source-message",
            expectedVersion = ExpectedVersion.NoStream,
            events = listOf(testEvent("SourceMessageRecorded", eventId = uuid(101))),
        )

        StepVerifier.create(eventStore.append(command))
            .expectNextCount(1)
            .verifyComplete()

        StepVerifier.create(eventStore.append(command))
            .assertNext { result ->
                check(result.previousStreamVersion == 0L)
                check(result.currentStreamVersion == 1L)
                check(result.events.single().eventId == uuid(101))
            }
            .verifyComplete()
    }

    @Test
    fun `reads events from multiple streams in strict global position order with limit`() {
        val eventStore = PostgreSqlEventStore(
            databaseClient = databaseClient,
            transactionalOperator = transactionalOperator,
            objectMapper = objectMapper,
            clock = fixedClock,
            streamIdGenerator = deterministicUuidGenerator(start = 60),
        )

        StepVerifier.create(
            eventStore.append(
                AppendEventsCommand(
                    streamName = "subject:00000000-0000-0000-0000-000000000001",
                    streamType = "subject",
                    expectedVersion = ExpectedVersion.NoStream,
                    events = listOf(testEvent("SubjectCreated", eventId = uuid(201))),
                ),
            ),
        ).expectNextCount(1).verifyComplete()

        StepVerifier.create(
            eventStore.append(
                AppendEventsCommand(
                    streamName = "task:00000000-0000-0000-0000-000000000001",
                    streamType = "task",
                    expectedVersion = ExpectedVersion.NoStream,
                    events = listOf(testEvent("TaskCreated", eventId = uuid(202))),
                ),
            ),
        ).expectNextCount(1).verifyComplete()

        StepVerifier.create(eventStore.readFrom(globalPosition = 0, limit = 1))
            .assertNext { event ->
                check(event.globalPosition == 1L)
                check(event.eventType == "SubjectCreated")
            }
            .verifyComplete()

        StepVerifier.create(eventStore.readFrom(globalPosition = 1, limit = 10).map { it.eventType })
            .expectNext("TaskCreated")
            .verifyComplete()
    }

    @Test
    fun `saves and loads projection offsets`() {
        val offsetStore = PostgreSqlProjectionOffsetStore(
            databaseClient = databaseClient,
            clock = fixedClock,
        )

        StepVerifier.create(offsetStore.load("tasks_projection"))
            .assertNext { offset ->
                check(offset.projectionName == "tasks_projection")
                check(offset.lastGlobalPosition == 0L)
                check(offset.updatedAt == Instant.EPOCH)
            }
            .verifyComplete()

        StepVerifier.create(offsetStore.save("tasks_projection", 42))
            .verifyComplete()

        StepVerifier.create(offsetStore.load("tasks_projection"))
            .assertNext { offset ->
                check(offset.projectionName == "tasks_projection")
                check(offset.lastGlobalPosition == 42L)
                check(offset.updatedAt == Instant.parse("2026-06-07T12:00:00Z"))
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
            assumeTrue(System.getenv("SSTT_EVENTSTORE_INTEGRATION_TESTS") == "true")

            val connectionFactory = ConnectionFactories.get(
                System.getenv("SSTT_TEST_R2DBC_URL")
                    ?: "r2dbc:postgresql://sstt:sstt@localhost:5432/sstt",
            )
            databaseClient = DatabaseClient.create(connectionFactory)
            transactionalOperator = TransactionalOperator.create(R2dbcTransactionManager(connectionFactory))
        }

        private fun resetEventstoreSchema() {
            val migration = Files.readString(
                Path.of("../migration/src/main/resources/db/migration/eventstore/V1__eventstore_schema.sql"),
            )
            databaseClient.sql("drop schema if exists eventstore cascade")
                .fetch()
                .rowsUpdated()
                .then(databaseClient.sql(migration).fetch().rowsUpdated())
                .block()
        }

        @JvmStatic
        @AfterAll
        fun cleanup() {
            if (
                System.getenv("SSTT_EVENTSTORE_INTEGRATION_TESTS") == "true" &&
                this::databaseClient.isInitialized
            ) {
                databaseClient.sql("drop schema if exists eventstore cascade")
                    .fetch()
                    .rowsUpdated()
                    .block()
            }
        }

        private fun deterministicUuidGenerator(start: Long): () -> UUID {
            var current = start
            return {
                current += 1
                UUID.fromString("00000000-0000-0000-0000-${current.toString(16).padStart(12, '0')}")
            }
        }

        private fun testEvent(eventType: String, eventId: UUID = UUID.randomUUID()): NewEvent {
            return NewEvent(
                eventId = eventId,
                eventType = eventType,
                eventVersion = 1,
                payload = objectMapper.createObjectNode(),
                metadata = objectMapper.createObjectNode(),
            )
        }

        private fun uuid(value: Long): UUID {
            return UUID.fromString("00000000-0000-0000-0000-${value.toString(16).padStart(12, '0')}")
        }
    }
}
