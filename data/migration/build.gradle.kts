plugins {
    id("sstt.kotlin-library-conventions")
}

dependencies {
    implementation(SsttLibraries.flywayCore)
    implementation(SsttLibraries.flywayPostgresql)
    runtimeOnly(SsttLibraries.postgresql)
}

tasks.register<JavaExec>("migrateLocal") {
    group = "database"
    description = "Runs all database migrations against the configured local PostgreSQL database."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.sstt.data.migration.RunMigrationsKt")
    args("migrate")
}

tasks.register<JavaExec>("flywayInfoLocal") {
    group = "database"
    description = "Prints Flyway migration status for the configured local PostgreSQL database."
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.sstt.data.migration.RunMigrationsKt")
    args("info")
}
