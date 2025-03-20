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


// TEMP
include(":libs:ktorfx:all")
include(":libs:ktorfx:core")
include(":libs:ktorfx:cluster")
include(":libs:ktorfx:logging")
include(":libs:ktorfx:insights")
include(":libs:ktorfx:rest")
include(":libs:ktorfx:staticweb")
include(":libs:ktorfx:messaging")
include(":libs:ktorfx:testing")
