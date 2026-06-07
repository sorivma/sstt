package com.sstt.data.identity

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.core.DatabaseClient

/**
 * Spring bean configuration for identity data infrastructure.
 *
 * The composition layer imports this configuration after eventstore
 * infrastructure is available. Feature/application code can then inject the
 * identity writer, read repository, and projection handler as ordinary beans.
 */
@Configuration(proxyBeanMethods = false)
class IdentityDataConfiguration {
    @Bean
    fun studentEventWriter(eventStore: EventStore, objectMapper: ObjectMapper): StudentEventWriter {
        return StudentEventWriter(eventStore, objectMapper)
    }

    @Bean
    fun studentReadRepository(databaseClient: DatabaseClient): StudentReadRepository {
        return StudentReadRepository(databaseClient)
    }

    @Bean
    fun studentProjection(databaseClient: DatabaseClient): StudentProjection {
        return StudentProjection(databaseClient)
    }
}
