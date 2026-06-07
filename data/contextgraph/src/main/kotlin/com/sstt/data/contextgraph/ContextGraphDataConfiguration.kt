package com.sstt.data.contextgraph

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.core.DatabaseClient

@Configuration(proxyBeanMethods = false)
class ContextGraphDataConfiguration {
    @Bean
    fun relationEventWriter(eventStore: EventStore, objectMapper: ObjectMapper): RelationEventWriter {
        return RelationEventWriter(eventStore, objectMapper)
    }

    @Bean
    fun relationReadRepository(databaseClient: DatabaseClient): RelationReadRepository {
        return RelationReadRepository(databaseClient)
    }

    @Bean
    fun relationProjection(databaseClient: DatabaseClient): RelationProjection {
        return RelationProjection(databaseClient)
    }
}
