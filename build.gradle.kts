plugins {
    java
    id("org.springframework.boot") version "3.5.0"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.sstt"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val isWindows = System.getProperty("os.name").lowercase().contains("windows")

tasks.register<Exec>("dockerComposeUp") {
    group = "application"
    description = "Starts the local PostgreSQL container and waits until it is healthy."

    if (isWindows) {
        commandLine("cmd", "/c", "docker compose up -d --wait postgres")
    } else {
        commandLine("sh", "-c", "docker compose up -d --wait postgres")
    }
}

tasks.named("test") {
    mustRunAfter("dockerComposeUp")
}

tasks.named("bootRun") {
    mustRunAfter("test")
}

tasks.register("localDev") {
    group = "application"
    description = "Starts local dependencies, runs tests, and launches the service with Flyway migrations."
    dependsOn("dockerComposeUp", "test", "bootRun")
}

