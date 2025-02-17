plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version Deps.kotlinVersion
    java
}

group = "io.peekandpoke"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))

    implementation(kotlin("reflect"))

    implementation(Deps.ultra_common_mp)

    implementation(Deps.ktor_client_core)
    implementation(Deps.ktor_client_okhttp)
    implementation(Deps.ktor_client_cio)

    implementation(Deps.kotlinx_serialization_core)
    implementation(Deps.kotlinx_serialization_json)

    implementation("com.typesafe:config:1.4.3")

    implementation(Deps.slf4j_api)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}
