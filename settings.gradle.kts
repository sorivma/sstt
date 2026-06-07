pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "sstt"

include(
    "app:web",
    "data:academics",
    "data:assistant",
    "data:contextgraph",
    "data:eventstore",
    "data:identity",
    "data:materials",
    "data:migration",
    "data:sources",
    "data:tasks",
)
