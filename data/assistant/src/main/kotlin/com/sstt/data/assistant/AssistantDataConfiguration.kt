package com.sstt.data.assistant

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.core.DatabaseClient

@Configuration(proxyBeanMethods = false)
class AssistantDataConfiguration {
    @Bean
    fun assistantEventWriter(eventStore: EventStore, objectMapper: ObjectMapper): AssistantEventWriter {
        return AssistantEventWriter(eventStore, objectMapper)
    }

    @Bean
    fun assistantReadRepository(databaseClient: DatabaseClient): AssistantReadRepository {
        return AssistantReadRepository(databaseClient)
    }

    @Bean
    fun assistantProjection(databaseClient: DatabaseClient): AssistantProjection {
        return AssistantProjection(databaseClient)
    }
}
