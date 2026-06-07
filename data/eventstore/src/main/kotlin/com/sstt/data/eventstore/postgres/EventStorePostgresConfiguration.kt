package com.sstt.data.eventstore.postgres

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import com.sstt.data.eventstore.api.ProjectionOffsetStore
import java.time.Clock
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.transaction.reactive.TransactionalOperator

/**
 * Spring bean configuration for PostgreSQL-backed event store infrastructure.
 *
 * Application and feature modules should depend on the [EventStore] and
 * [ProjectionOffsetStore] interfaces. This configuration is imported by the
 * composition layer to bind those ports to the R2DBC PostgreSQL implementation.
 * A custom [Clock] bean may be provided by the application; otherwise UTC system
 * time is used.
 */
@Configuration(proxyBeanMethods = false)
class EventStorePostgresConfiguration {
    /**
     * Exposes the append/read event store port backed by PostgreSQL.
     */
    @Bean
    fun eventStore(
        databaseClient: DatabaseClient,
        transactionalOperator: TransactionalOperator,
        objectMapper: ObjectMapper,
        clockProvider: ObjectProvider<Clock>,
    ): EventStore {
        return PostgreSqlEventStore(
            databaseClient = databaseClient,
            transactionalOperator = transactionalOperator,
            objectMapper = objectMapper,
            clock = clockProvider.getIfAvailable { Clock.systemUTC() },
        )
    }

    /**
     * Exposes projection checkpoint storage backed by PostgreSQL.
     */
    @Bean
    fun projectionOffsetStore(
        databaseClient: DatabaseClient,
        clockProvider: ObjectProvider<Clock>,
    ): ProjectionOffsetStore {
        return PostgreSqlProjectionOffsetStore(
            databaseClient = databaseClient,
            clock = clockProvider.getIfAvailable { Clock.systemUTC() },
        )
    }
}
