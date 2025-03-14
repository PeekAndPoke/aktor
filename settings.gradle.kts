pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal() // Helps resolve plugins like 'kotlinx-serialization'
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "aktor"

include(":frontend")
