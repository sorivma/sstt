plugins {
    id("sstt.kotlin-library-conventions")
    id("org.springframework.boot")
}

dependencies {
    "implementation"(SsttLibraries.kotlinReflect)
    "implementation"(SsttLibraries.springBootStarterThymeleaf)
    "implementation"(SsttLibraries.springBootStarterValidation)
    "implementation"(SsttLibraries.springBootStarterWebflux)
    "implementation"(SsttLibraries.springBootStarterDataR2dbc)
}
