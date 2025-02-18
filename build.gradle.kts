plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version Deps.kotlinVersion
    java
    idea
}

group = "io.peekandpoke"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(kotlin("reflect"))

    implementation(Deps.ultra_common_mp)

    implementation(Deps.ktor_client_core)
    implementation(Deps.ktor_client_okhttp)
    implementation(Deps.ktor_client_cio)
    implementation("io.ktor:ktor-client-content-negotiation:${Deps.ktor_version}")
    implementation("io.ktor:ktor-client-plugins:${Deps.ktor_version}")
    implementation("io.ktor:ktor-client-logging:${Deps.ktor_version}")
    implementation("io.ktor:ktor-serialization-jackson:${Deps.ktor_version}")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    implementation(Deps.kotlinx_serialization_core)
    implementation(Deps.kotlinx_serialization_json)

    implementation("com.typesafe:config:1.4.3")
    implementation("com.aallam.openai:openai-client:3.8.2")

    implementation(Deps.slf4j_api)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
