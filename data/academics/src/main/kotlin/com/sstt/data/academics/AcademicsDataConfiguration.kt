package com.sstt.data.academics

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.core.DatabaseClient

@Configuration(proxyBeanMethods = false)
class AcademicsDataConfiguration {
    @Bean
    fun academicsEventWriter(eventStore: EventStore, objectMapper: ObjectMapper): AcademicsEventWriter {
        return AcademicsEventWriter(eventStore, objectMapper)
    }

    @Bean
    fun academicsReadRepository(databaseClient: DatabaseClient): AcademicsReadRepository {
        return AcademicsReadRepository(databaseClient)
    }

    @Bean
    fun academicsProjection(databaseClient: DatabaseClient): AcademicsProjection {
        return AcademicsProjection(databaseClient)
    }
}
