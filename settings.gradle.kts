pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "zombie-client"

include(":app")

include(":shared", ":cast")

project(":shared").projectDir = file("../android-shared")

project(":cast").projectDir = file("../zombie-aircast-android")
