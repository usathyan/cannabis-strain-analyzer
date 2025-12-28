pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "BudMash"
include(":shared")
include(":androidApp")
include(":composeApp")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
