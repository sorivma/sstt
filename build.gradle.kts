group = "com.sstt"
version = "0.0.1-SNAPSHOT"

tasks.register<Exec>("dockerComposeUp") {
    group = "local development"
    description = "Starts local PostgreSQL and waits until it is healthy."

    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    if (isWindows) {
        commandLine("cmd", "/c", "docker compose up -d --wait postgres")
    } else {
        commandLine("sh", "-c", "docker compose up -d --wait postgres")
    }
}

tasks.register<Exec>("dockerComposeDown") {
    group = "local development"
    description = "Stops local Docker Compose services without deleting volumes."

    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    if (isWindows) {
        commandLine("cmd", "/c", "docker compose down")
    } else {
        commandLine("sh", "-c", "docker compose down")
    }
}

tasks.register<Exec>("dockerComposeLogs") {
    group = "local development"
    description = "Prints local Docker Compose service logs."

    val isWindows = System.getProperty("os.name").lowercase().contains("windows")
    if (isWindows) {
        commandLine("cmd", "/c", "docker compose logs postgres")
    } else {
        commandLine("sh", "-c", "docker compose logs postgres")
    }
}

tasks.register("localDevData") {
    group = "local development"
    description = "Starts local storage dependencies and verifies data modules."
    dependsOn("dockerComposeUp", "check")
}

tasks.register("localDevWeb") {
    group = "local development"
    description = "Starts local storage dependencies, runs migrations, and starts the web application."
    dependsOn("dockerComposeUp", ":data:migration:migrateLocal", ":app:web:bootRun")
}

gradle.projectsEvaluated {
    tasks.getByPath(":data:migration:migrateLocal").mustRunAfter(tasks.getByPath(":dockerComposeUp"))
    tasks.getByPath(":app:web:bootRun").mustRunAfter(tasks.getByPath(":data:migration:migrateLocal"))
}
