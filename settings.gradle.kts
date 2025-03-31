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

include(":libs:shared")

include(":utils:crawl4ai")
include(":utils:geo")

include(":libs:reaktor:auth")
