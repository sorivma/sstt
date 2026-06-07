package com.sstt.data.tasks

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.core.DatabaseClient

@Configuration(proxyBeanMethods = false)
class TasksDataConfiguration {
    @Bean
    fun taskEventWriter(eventStore: EventStore, objectMapper: ObjectMapper): TaskEventWriter {
        return TaskEventWriter(eventStore, objectMapper)
    }

    @Bean
    fun taskReadRepository(databaseClient: DatabaseClient): TaskReadRepository {
        return TaskReadRepository(databaseClient)
    }

    @Bean
    fun taskProjection(databaseClient: DatabaseClient): TaskProjection {
        return TaskProjection(databaseClient)
    }
}
