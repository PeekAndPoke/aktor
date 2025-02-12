plugins {
    kotlin("jvm")
    kotlin("plugin.serialization") version Deps.kotlinVersion
}

group = "io.peekandpoke"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-client-cio-jvm:3.0.3")
    testImplementation(kotlin("test"))

    api(Deps.ultra_common_mp)

    implementation(Deps.ktor_client_core)
    implementation(Deps.ktor_client_okhttp)
    implementation(Deps.ktor_client_cio)

    implementation(Deps.kotlinx_serialization_core)
    implementation(Deps.kotlinx_serialization_json)

    implementation(Deps.slf4j_api)
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
}
