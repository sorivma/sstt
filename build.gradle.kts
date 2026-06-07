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

tasks.register("localDevData") {
    group = "local development"
    description = "Starts local storage dependencies and verifies data modules."
    dependsOn("dockerComposeUp", "check")
}
