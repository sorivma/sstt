package com.sstt.data.sources

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.core.DatabaseClient

@Configuration(proxyBeanMethods = false)
class SourcesDataConfiguration {
    @Bean
    fun sourceMessageEventWriter(eventStore: EventStore, objectMapper: ObjectMapper): SourceMessageEventWriter {
        return SourceMessageEventWriter(eventStore, objectMapper)
    }

    @Bean
    fun sourceMessageReadRepository(databaseClient: DatabaseClient): SourceMessageReadRepository {
        return SourceMessageReadRepository(databaseClient)
    }

    @Bean
    fun sourceMessageProjection(databaseClient: DatabaseClient): SourceMessageProjection {
        return SourceMessageProjection(databaseClient)
    }
}
