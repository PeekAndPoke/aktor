@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests
import Deps.Test.jvmTestDeps

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version Deps.kotlinVersion
    id("com.google.devtools.ksp") version Deps.Ksp.version
    java
    idea
    application
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

application {
    mainClass.set(
        if (project.hasProperty("mainClass")) {
            project.property("mainClass").toString()
        } else {
            "io.peekandpoke.aktor.MainKt"
        }
    )
}

kotlin {
    jvmToolchain(Deps.jvmTargetVersion)

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

        // OpenAI-Client
        // https://central.sonatype.com/artifact/com.aallam.openai/openai-client
        implementation("com.aallam.openai:openai-client:4.0.1")
        // Anthropic Client
        // https://central.sonatype.com/artifact/com.anthropic/anthropic-java
        implementation("com.anthropic:anthropic-java:1.0.0")

        // https://github.com/open-meteo/open-meteo-api-kotlin/wiki/Installation
        implementation("com.open-meteo:open-meteo-api-kotlin:0.7.1-beta.1")

        // https://mvnrepository.com/artifact/org.mariuszgromada.math/MathParser.org-mXparser
        implementation("org.mariuszgromada.math:MathParser.org-mXparser:6.1.0")

        // https://mvnrepository.com/artifact/io.modelcontextprotocol/kotlin-sdk
        implementation("io.modelcontextprotocol:kotlin-sdk:0.3.0")

        Deps.JavaLibs.Jackson.fullImpl(this)

        implementation(Deps.JavaLibs.logback_classic)

        implementation(Deps.KotlinLibs.Karango.core)
        implementation(Deps.KotlinLibs.Karango.addons)
        ksp(Deps.KotlinLibs.Karango.ksp)

        implementation(project(":libs:shared"))
        implementation(project(":utils:crawl4ai"))
        implementation(project(":utils:geo"))

        implementation(Deps.KotlinLibs.Funktor.all)

        implementation(project(":libs:reaktor:auth"))


        // TEST /////////////////////////////////////////////////////////
        testImplementation(Deps.Ktor.Server.Test.host)
        jvmTestDeps()
    }
}

tasks {
    configureJvmTests()

    named<JavaExec>("run") {
        standardInput = System.`in`
    }
}
