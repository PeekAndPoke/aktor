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
        implementation(Deps.KotlinX.serialization_core)
        implementation(Deps.KotlinX.serialization_json)

        implementation(Deps.KotlinLibs.Ultra.common)
        implementation(Deps.KotlinLibs.Ultra.slumber)

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

        Deps.JavaLibs.Jackson.fullImpl(this)

        implementation(Deps.JavaLibs.slf4j_api)

        implementation(project(":frontend"))

        Deps.Test {
            jvmTestDeps()
        }
    }
}

tasks {
    configureJvmTests()
}
