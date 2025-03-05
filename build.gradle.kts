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
    mavenLocal()
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

    implementation(Deps.ultra_common)

    implementation(Deps.ktor_client_core)
    implementation(Deps.ktor_client_okhttp)
    implementation(Deps.ktor_client_cio)
    implementation(Deps.ktor_client_content_negotiation)
    implementation(Deps.ktor_client_plugins)
    implementation(Deps.ktor_client_logging)

    implementation(Deps.ktor_serialization_kotlinx_json)
    implementation(Deps.ktor_serialization_jackson)

    implementation(Deps.jackson_databind)
    implementation(Deps.jackson_module_kotlin)

    implementation(Deps.kotlinx_serialization_core)
    implementation(Deps.kotlinx_serialization_json)

    implementation("com.typesafe:config:1.4.3")
    implementation("com.aallam.openai:openai-client:4.0.1")

    implementation(Deps.slf4j_api)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
