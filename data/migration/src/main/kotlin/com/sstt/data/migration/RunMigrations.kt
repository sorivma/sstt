package com.sstt.data.migration

import org.flywaydb.core.Flyway

/**
 * Small command-line entrypoint for running project database migrations without
 * starting the application.
 *
 * The migration module is global for the whole data layer. It owns Flyway SQL
 * resources and exposes Gradle tasks that can migrate a local PostgreSQL
 * database using the same defaults as docker-compose.yml.
 */
fun main(args: Array<String>) {
    val command = args.firstOrNull() ?: "migrate"
    val flyway = Flyway.configure()
        .dataSource(
            env("SSTT_MIGRATION_URL", "jdbc:postgresql://localhost:5432/sstt"),
            env("SSTT_MIGRATION_USER", "sstt"),
            env("SSTT_MIGRATION_PASSWORD", "sstt"),
        )
        .locations(env("SSTT_MIGRATION_LOCATIONS", "classpath:db/migration"))
        .baselineOnMigrate(false)
        .load()

    when (command) {
        "migrate" -> {
            val result = flyway.migrate()
            println("Applied ${result.migrationsExecuted} migration(s). Current version: ${result.targetSchemaVersion}")
        }
        "info" -> {
            flyway.info().all().forEach { migration ->
                println("${migration.version ?: "-"} ${migration.description} ${migration.state}")
            }
        }
        else -> error("Unknown migration command '$command'. Supported commands: migrate, info.")
    }
}

private fun env(name: String, default: String): String {
    return System.getenv(name)?.takeIf { it.isNotBlank() } ?: default
}
