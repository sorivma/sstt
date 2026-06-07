plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(SsttVersions.javaLanguage)
}

dependencies {
    "implementation"(platform(SsttLibraries.springBootBom))
    "testImplementation"(platform(SsttLibraries.springBootBom))
    "testImplementation"(SsttLibraries.springBootStarterTest)
    "testRuntimeOnly"(SsttLibraries.junitPlatformLauncher)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
