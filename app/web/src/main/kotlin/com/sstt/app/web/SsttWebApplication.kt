package com.sstt.app.web

import com.sstt.data.eventstore.postgres.EventStorePostgresConfiguration
import com.sstt.data.academics.AcademicsDataConfiguration
import com.sstt.data.assistant.AssistantDataConfiguration
import com.sstt.data.contextgraph.ContextGraphDataConfiguration
import com.sstt.data.identity.IdentityDataConfiguration
import com.sstt.data.materials.MaterialsDataConfiguration
import com.sstt.data.sources.SourcesDataConfiguration
import com.sstt.data.tasks.TasksDataConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import

/**
 * Spring Boot composition module for the SSTT web application.
 *
 * This module wires reusable data modules into an executable web surface. The
 * data modules keep storage contracts and implementations; this application
 * owns HTTP controllers, templates, runtime properties, and bootstrapping.
 */
@SpringBootApplication
@Import(
    EventStorePostgresConfiguration::class,
    AcademicsDataConfiguration::class,
    AssistantDataConfiguration::class,
    ContextGraphDataConfiguration::class,
    IdentityDataConfiguration::class,
    MaterialsDataConfiguration::class,
    SourcesDataConfiguration::class,
    TasksDataConfiguration::class,
)
class SsttWebApplication

fun main(args: Array<String>) {
    runApplication<SsttWebApplication>(*args)
}
