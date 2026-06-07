plugins {
    id("sstt.spring-boot-application-conventions")
}

dependencies {
    implementation(project(":data:eventstore"))
    implementation(project(":data:identity"))
    implementation(project(":data:migration"))
}
