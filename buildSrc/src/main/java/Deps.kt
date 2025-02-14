import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.TaskContainerScope
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

@Suppress("MemberVisibilityCanBePrivate", "ConstPropertyName")
object Deps {
    operator fun invoke(block: Deps.() -> Unit) {
        this.block()
    }

    // Kotlin ////////////////////////////////////////////////////////////////////////////////////
    const val kotlinVersion = "1.9.25"
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // JVM ///////////////////////////////////////////////////////////////////////////////////////
    val jvmTarget = JvmTarget.JVM_17
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // Dokka /////////////////////////////////////////////////////////////////////////////////////
    // https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-gradle-plugin
    // Dokka gradle plugin org.jetbrains.dokka
    const val dokkaVersion = "1.9.20" // kotlinVersion
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // Publishing ////////////////////////////////////////////////////////////////////////////////
    // https://search.maven.org/artifact/com.vanniktech/gradle-maven-publish-plugin
    const val mavenPublishVersion = "0.30.0"
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // KOTLIN - DEPS ///////////////////////////////////////////////////////////////////////////////////////////////////

    // https://search.maven.org/search?q=g:io.peekandpoke.ultra%20AND%20a:commonmp
    private const val ultra_version = "0.77.0"
    const val ultra_common_mp = "io.peekandpoke.ultra:commonmp:$ultra_version"

    // https://kotlinlang.org/docs/releases.html#release-details
    // https://github.com/Kotlin/kotlinx.coroutines/releases
    private const val kotlinx_coroutines_version = "1.8.1"
    const val kotlinx_coroutines_core =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinx_coroutines_version"
    const val kotlinx_coroutines_core_js =
        "org.jetbrains.kotlinx:kotlinx-coroutines-core-js:$kotlinx_coroutines_version"

    // https://kotlinlang.org/docs/releases.html#release-details
    // https://github.com/Kotlin/kotlinx.serialization/releases
    private const val kotlinx_serialization_version = "1.6.3"
    const val kotlinx_serialization_core =
        "org.jetbrains.kotlinx:kotlinx-serialization-core:$kotlinx_serialization_version"
    const val kotlinx_serialization_json =
        "org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinx_serialization_version"

    // https://kotlinlang.org/docs/releases.html
    // https://github.com/ktorio/ktor/releases
    const val ktor_version = "2.3.12"
    const val ktor_client_core = "io.ktor:ktor-client-core:$ktor_version"
    const val ktor_client_cio = "io.ktor:ktor-client-cio:$ktor_version"
    const val ktor_client_okhttp = "io.ktor:ktor-client-okhttp:$ktor_version"


    // https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-datetime
    private const val kotlinx_datetime_version = "0.6.1"
    const val kotlinx_datetime_common = "org.jetbrains.kotlinx:kotlinx-datetime:$kotlinx_datetime_version"

    // JAVA - DEPS /////////////////////////////////////////////////////////////////////////////////////////////////////

    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    private const val slf4j_version = "2.0.16"
    const val slf4j_api = "org.slf4j:slf4j-api:$slf4j_version"

    // // NPM dependencies /////////////////////////////////////////////////////////////////////////

    object Npm {
        operator fun <T> invoke(block: Npm.() -> T): T {
            return this.block()
        }
    }

    // // Test dependencies ////////////////////////////////////////////////////////////////////////

    object Test {

        operator fun invoke(block: Test.() -> Unit) {
            this.block()
        }

        // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
        const val logback_version = "1.5.16"
        const val logback_classic = "ch.qos.logback:logback-classic:$logback_version"

        // https://mvnrepository.com/artifact/io.kotest/kotest-common
        const val kotest_plugin_version = "5.9.1"
        const val kotest_version = "5.9.1"
//        const val kotest_version = "5.9.0.1440-SNAPSHOT"

        const val kotest_assertions_core = "io.kotest:kotest-assertions-core:$kotest_version"
        const val kotest_framework_api = "io.kotest:kotest-framework-api:$kotest_version"
        const val kotest_framework_datatest = "io.kotest:kotest-framework-datatest:$kotest_version"
        const val kotest_framework_engine = "io.kotest:kotest-framework-engine:$kotest_version"

        const val kotest_runner_junit_jvm = "io.kotest:kotest-runner-junit5-jvm:$kotest_version"

        fun KotlinDependencyHandler.commonTestDeps() {
            kotlin("test-common")
            kotlin("test-annotations-common")
            implementation(kotest_assertions_core)
            implementation(kotest_framework_api)
            implementation(kotest_framework_datatest)
            implementation(kotest_framework_engine)
        }

        fun KotlinDependencyHandler.jsTestDeps() {
            implementation(kotest_assertions_core)
            implementation(kotest_framework_api)
            implementation(kotest_framework_datatest)
            implementation(kotest_framework_engine)
        }

        fun KotlinDependencyHandler.jvmTestDeps() {
            implementation(logback_classic)
            implementation(kotest_runner_junit_jvm)
            implementation(kotest_assertions_core)
            implementation(kotest_framework_api)
            implementation(kotest_framework_datatest)
            implementation(kotest_framework_engine)
        }

        fun DependencyHandlerScope.jvmTestDeps() {
            testImplementation(logback_classic)
            testImplementation(kotest_runner_junit_jvm)
            testImplementation(kotest_assertions_core)
            testImplementation(kotest_framework_api)
            testImplementation(kotest_framework_engine)
        }

        fun TaskContainerScope.configureJvmTests(
            configure: org.gradle.api.tasks.testing.Test.() -> Unit = {},
        ) {
            withType(org.gradle.api.tasks.testing.Test::class.java).configureEach {
                useJUnitPlatform()

                filter {
                    isFailOnNoMatchingTests = false
                }

//                testLogging {
//                    showExceptions = true
//                    showStandardStreams = true
//                    events = setOf(
//                        org.gradle.api.tasks.testing.logging.TestLogEvent.FAILED,
//                        org.gradle.api.tasks.testing.logging.TestLogEvent.PASSED
//                    )
//                    exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
//                }

                configure()
            }
        }
    }
}

private fun DependencyHandlerScope.testImplementation(dep: String) =
    add("testImplementation", dep)
