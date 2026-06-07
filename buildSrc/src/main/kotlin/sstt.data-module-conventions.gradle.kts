plugins {
    id("sstt.kotlin-library-conventions")
}

dependencies {
    "api"(SsttLibraries.springJdbc)
    "implementation"(SsttLibraries.springTx)
    "implementation"(SsttLibraries.flywayCore)
    "implementation"(SsttLibraries.flywayPostgresql)
    "runtimeOnly"(SsttLibraries.postgresql)
    "testRuntimeOnly"(SsttLibraries.h2)
}
