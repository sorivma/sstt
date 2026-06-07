package com.sstt.data.materials

import com.fasterxml.jackson.databind.ObjectMapper
import com.sstt.data.eventstore.api.EventStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.r2dbc.core.DatabaseClient

@Configuration(proxyBeanMethods = false)
class MaterialsDataConfiguration {
    @Bean
    fun materialEventWriter(eventStore: EventStore, objectMapper: ObjectMapper): MaterialEventWriter {
        return MaterialEventWriter(eventStore, objectMapper)
    }

    @Bean
    fun materialReadRepository(databaseClient: DatabaseClient): MaterialReadRepository {
        return MaterialReadRepository(databaseClient)
    }

    @Bean
    fun materialProjection(databaseClient: DatabaseClient): MaterialProjection {
        return MaterialProjection(databaseClient)
    }
}
