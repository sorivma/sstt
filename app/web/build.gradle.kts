plugins {
    id("sstt.spring-boot-application-conventions")
}

val buildFrontendAssets by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    val npmCommand = if (System.getProperty("os.name").lowercase().contains("windows")) "npm.cmd" else "npm"
    commandLine(npmCommand, "run", "assets:build")
    inputs.files(
        rootProject.file("package.json"),
        rootProject.file("package-lock.json"),
    )
    inputs.dir(project.layout.projectDirectory.dir("src/main/resources/assets"))
    inputs.dir(project.layout.projectDirectory.dir("src/main/resources/templates"))
    outputs.file(project.layout.projectDirectory.file("src/main/resources/static/css/app.css"))
    outputs.file(project.layout.projectDirectory.file("src/main/resources/static/vendor/lucide/sprite.svg"))
}

tasks.processResources {
    dependsOn(buildFrontendAssets)
}

dependencies {
    implementation(project(":data:academics"))
    implementation(project(":data:assistant"))
    implementation(project(":data:contextgraph"))
    implementation(project(":data:eventstore"))
    implementation(project(":data:identity"))
    implementation(project(":data:materials"))
    implementation(project(":data:migration"))
    implementation(project(":data:sources"))
    implementation(project(":data:tasks"))
}
