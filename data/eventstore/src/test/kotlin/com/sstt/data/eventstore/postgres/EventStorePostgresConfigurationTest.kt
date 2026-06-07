package com.sstt.data.eventstore.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.api.ProjectionOffsetStore
import io.r2dbc.spi.ConnectionFactories
import io.r2dbc.spi.ConnectionFactory
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.reactive.TransactionalOperator

/**
 * Unit test for Spring bean wiring exposed by the event store PostgreSQL
 * configuration.
 */
class EventStorePostgresConfigurationTest {
    @Test
    fun `configuration exposes event store ports as spring beans`() {
        AnnotationConfigApplicationContext().use { context ->
            context.register(TestDependencies::class.java, EventStorePostgresConfiguration::class.java)
            context.refresh()

            check(context.getBean(EventStore::class.java) is PostgreSqlEventStore)
            check(context.getBean(ProjectionOffsetStore::class.java) is PostgreSqlProjectionOffsetStore)
        }
    }

    @Configuration(proxyBeanMethods = false)
    private class TestDependencies {
        @Bean
        fun objectMapper(): ObjectMapper = ObjectMapper()

        @Bean
        fun clock(): Clock = Clock.fixed(Instant.parse("2026-06-07T12:00:00Z"), ZoneOffset.UTC)

        @Bean
        fun connectionFactory(): ConnectionFactory {
            return ConnectionFactories.get("r2dbc:postgresql://sstt:sstt@localhost:5432/sstt")
        }

        @Bean
        fun databaseClient(connectionFactory: ConnectionFactory): DatabaseClient {
            return DatabaseClient.create(connectionFactory)
        }

        @Bean
        fun transactionalOperator(connectionFactory: ConnectionFactory): TransactionalOperator {
            return TransactionalOperator.create(R2dbcTransactionManager(connectionFactory))
        }
    }

}
