@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version Deps.kotlinVersion
    java
    idea
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

allprojects {
    repositories {
        mavenCentral()
        maven { setUrl("https://jitpack.io") }
        mavenLocal()
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

kotlin {
    jvmToolchain(21)

    dependencies {
        implementation(kotlin("reflect"))
        implementation(Deps.KotlinX.coroutines_core)
        implementation(Deps.KotlinX.coroutines_reactor)
        implementation(Deps.KotlinX.serialization_core)
        implementation(Deps.KotlinX.serialization_json)
        implementation(Deps.KotlinX.atomicfu)

        implementation(Deps.KotlinLibs.Ultra.common)
        implementation(Deps.KotlinLibs.Ultra.slumber)
        implementation(Deps.KotlinLibs.Ultra.kontainer)

        Deps.Ktor.Server.full(this)

        implementation(Deps.Ktor.Client.core)
        implementation(Deps.Ktor.Client.okhttp)
        implementation(Deps.Ktor.Client.cio)
        implementation(Deps.Ktor.Client.content_negotiation)
        implementation(Deps.Ktor.Client.plugins)
        implementation(Deps.Ktor.Client.logging)

        implementation(Deps.Ktor.Common.serialization_kotlinx_json)
        implementation(Deps.Ktor.Common.serialization_jackson)

        implementation("com.typesafe:config:1.4.3")
        implementation("com.aallam.openai:openai-client:4.0.1")

        // https://github.com/open-meteo/open-meteo-api-kotlin/wiki/Installation
        implementation("com.open-meteo:open-meteo-api-kotlin:0.7.1-beta.1")

        // https://mvnrepository.com/artifact/io.modelcontextprotocol/kotlin-sdk
        implementation("io.modelcontextprotocol:kotlin-sdk:0.3.0")

        Deps.JavaLibs.Jackson.fullImpl(this)

        implementation(Deps.JavaLibs.logback_classic)
//        implementation(Deps.JavaLibs.slf4j_api)

        implementation(project(":utils:crawl4ai"))
        implementation(project(":frontend"))

        Deps.Test {
            jvmTestDeps()
        }
    }
}
dependencies {
    implementation("io.ktor:ktor-client-cio-jvm:3.1.1")
    implementation("io.ktor:ktor-client-okhttp-jvm:3.1.1")
    implementation("io.ktor:ktor-server-core:3.1.1")
    implementation("io.ktor:ktor-server-sse:3.1.1")
    implementation("io.ktor:ktor-server-core:3.1.1")
}

tasks {
    configureJvmTests()
}
