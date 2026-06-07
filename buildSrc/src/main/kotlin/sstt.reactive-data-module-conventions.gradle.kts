plugins {
    id("sstt.kotlin-library-conventions")
}

dependencies {
    "api"(SsttLibraries.reactorCore)
    "api"(SsttLibraries.jacksonDatabind)
    "api"(SsttLibraries.springContext)
    "api"(SsttLibraries.springR2dbc)
    "implementation"(SsttLibraries.springTx)
    "implementation"(SsttLibraries.r2dbcPostgresql)
    "testImplementation"(SsttLibraries.reactorTest)
    "testImplementation"(SsttLibraries.r2dbcPool)
    "testRuntimeOnly"(SsttLibraries.h2)
}
