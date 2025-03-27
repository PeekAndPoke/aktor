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
    const val kotlinVersion = "2.1.10"

    object Ksp {
        // https://github.com/google/ksp/releases
        const val version = "2.1.10-1.0.31"
        const val symbol_processing = "com.google.devtools.ksp:symbol-processing-api:$version"

        // https://mvnrepository.com/artifact/com.github.tschuchortdev/kotlin-compile-testing
        private const val compiletesting_version = "1.6.0"
        const val compiletesting = "com.github.tschuchortdev:kotlin-compile-testing:$compiletesting_version"
        const val compiletesting_ksp = "com.github.tschuchortdev:kotlin-compile-testing-ksp:$compiletesting_version"
    }

    // ///////////////////////////////////////////////////////////////////////////////////////////

    // JVM ///////////////////////////////////////////////////////////////////////////////////////
    val jvmTarget = JvmTarget.JVM_17
    val jvmTargetVersion = jvmTarget.target.toInt()
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // Dokka /////////////////////////////////////////////////////////////////////////////////////
    // https://mvnrepository.com/artifact/org.jetbrains.dokka/dokka-gradle-plugin
    // Dokka gradle plugin org.jetbrains.dokka
    const val dokkaVersion = "2.0.0" // kotlinVersion
    // ///////////////////////////////////////////////////////////////////////////////////////////

    // Publishing ////////////////////////////////////////////////////////////////////////////////
    // https://search.maven.org/artifact/com.vanniktech/gradle-maven-publish-plugin
    const val mavenPublishVersion = "0.30.0"
    // ///////////////////////////////////////////////////////////////////////////////////////////

    object KotlinLibs {
        private const val ultra_version = "0.84.0"
        private const val kraft_version = "0.64.0"

        object Ultra {
            // https://search.maven.org/search?q=g:io.peekandpoke.ultra%20AND%20a:commonmp
            const val common = "io.peekandpoke.ultra:common:$ultra_version"
            const val kontainer = "io.peekandpoke.ultra:kontainer:$ultra_version"
            const val logging = "io.peekandpoke.ultra:logging:$ultra_version"
            const val meta = "io.peekandpoke.ultra:meta:$ultra_version"
            const val security = "io.peekandpoke.ultra:security:$ultra_version"
            const val slumber = "io.peekandpoke.ultra:slumber:$ultra_version"
            const val vault = "io.peekandpoke.ultra:vault:$ultra_version"
        }

        object Kraft {
            // https://central.sonatype.com/search?q=g%3Aio.peekandpoke.kraft++a%3Acore&smo=true
            const val core = "io.peekandpoke.kraft:core:$kraft_version"
            const val semanticui = "io.peekandpoke.kraft:semanticui:$kraft_version"

            const val addons_chartjs = "io.peekandpoke.kraft:addons-chartjs:$kraft_version"
            const val addons_konva = "io.peekandpoke.kraft:addons-konva:$kraft_version"
            const val addons_marked = "io.peekandpoke.kraft:addons-marked:$kraft_version"
            const val addons_nxcompile = "io.peekandpoke.kraft:addons-nxcompile:$kraft_version"
            const val addons_pdfjs = "io.peekandpoke.kraft:addons-pdfjs:$kraft_version"
            const val addons_prismjs = "io.peekandpoke.kraft:addons-prismjs:$kraft_version"
            const val addons_signaturepad = "io.peekandpoke.kraft:addons-signaturepad:$kraft_version"
            const val addons_sourcemappedstacktrace =
                "io.peekandpoke.kraft:addons-sourcemappedstacktrace:$kraft_version"
        }

        object Karango {
            const val core = "io.peekandpoke.karango:core:$ultra_version"
            const val addons = "io.peekandpoke.karango:addons:$ultra_version"
            const val ksp = "io.peekandpoke.karango:ksp:$ultra_version"
        }

        // https://mvnrepository.com/artifact/com.github.ajalt.clikt/clikt
        private const val clikt_version = "5.0.3"
        const val clikt = "com.github.ajalt.clikt:clikt:$clikt_version"

        // https://mvnrepository.com/artifact/com.github.doyaaaaaken/kotlin-csv
        private const val csv_version = "1.10.0"
        const val csv = "com.github.doyaaaaaken:kotlin-csv:$csv_version"

        // https://mvnrepository.com/artifact/io.github.evanrupert/excelkt
        private const val excelkt_version = "1.0.2"
        const val excelkt = "io.github.evanrupert:excelkt:$excelkt_version"

        // https://mvnrepository.com/artifact/com.benasher44/uuid
        private const val uuid_version = "0.8.4"
        const val uuid = "com.benasher44:uuid:$uuid_version"

        // https://mvnrepository.com/artifact/io.github.g0dkar/qrcode-kotlin
        private const val qrcode_version = "4.3.0"
        const val qrcode = "io.github.g0dkar:qrcode-kotlin:$qrcode_version"

        // https://mvnrepository.com/artifact/io.github.serpro69/kotlin-faker
        private const val faker_version = "1.16.0"
        const val faker = "io.github.serpro69:kotlin-faker:$faker_version"
    }

    object KotlinX {
        // https://github.com/Kotlin/kotlinx.coroutines/releases
        private const val coroutines_version = "1.10.1"
        const val coroutines_core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version"
        const val coroutines_reactor = "org.jetbrains.kotlinx:kotlinx-coroutines-reactor:$coroutines_version"

        // https://github.com/Kotlin/kotlinx.serialization/releases
        private const val serialization_version = "1.8.0"
        const val serialization_core =
            "org.jetbrains.kotlinx:kotlinx-serialization-core:$serialization_version"
        const val serialization_json =
            "org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version"

        const val atomicfu_version = "0.23.2"
        const val atomicfu = "org.jetbrains.kotlinx:atomicfu:$atomicfu_version"

        // https://maven.pkg.jetbrains.space/public/p/kotlinx-html/maven/org/jetbrains/kotlinx/kotlinx-html/
        private const val html_version = "0.12.0"
        const val html = "org.jetbrains.kotlinx:kotlinx-html:$html_version"

        // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-css
        private const val wrappers_version = "2025.3.2"
        const val wrappers_css =
            "org.jetbrains.kotlin-wrappers:kotlin-css:$wrappers_version"

        // NOTICE: KEEP the pre.450 as newer versions do NOT support JAVA eight anymore
        // https://mvnrepository.com/artifact/org.jetbrains.kotlin-wrappers/kotlin-extensions
        private const val wrappers_extensions_version = "1.0.1-pre.823"
        const val wrappers_extensions =
            "org.jetbrains.kotlin-wrappers:kotlin-extensions:$wrappers_extensions_version"
    }

    object Ktor {
        // https://kotlinlang.org/docs/releases.html
        // https://github.com/ktorio/ktor/releases
        const val ktor_version = "3.1.1"

        object Server {
            object Test {
                const val host = "io.ktor:ktor-server-test-host:$ktor_version"
            }

            const val auth = "io.ktor:ktor-server-auth:$ktor_version"
            const val auth_jwt = "io.ktor:ktor-server-auth-jwt:$ktor_version"
            const val auto_head = "io.ktor:ktor-server-auto-head-response:$ktor_version"
            const val caching_headers = "io.ktor:ktor-server-caching-headers:$ktor_version"
            const val content_negotiation = "io.ktor:ktor-server-content-negotiation:$ktor_version"
            const val compression = "io.ktor:ktor-server-compression:$ktor_version"
            const val core = "io.ktor:ktor-server-core:$ktor_version"
            const val cors = "io.ktor:ktor-server-cors:$ktor_version"
            const val default_headers = "io.ktor:ktor-server-default-headers:$ktor_version"
            const val hsts = "io.ktor:ktor-server-hsts:$ktor_version"
            const val html_builder = "io.ktor:ktor-server-html-builder:$ktor_version"
            const val host_common = "io.ktor:ktor-server-host-common:$ktor_version"
            const val netty = "io.ktor:ktor-server-netty:$ktor_version"
            const val metrics = "io.ktor:ktor-server-metrics:$ktor_version"
            const val partial_content = "io.ktor:ktor-server-partial-content:$ktor_version"
            const val sessions = "io.ktor:ktor-server-sessions:$ktor_version"
            const val sse = "io.ktor:ktor-server-sse:$ktor_version"
            const val status_pages = "io.ktor:ktor-server-status-pages:$ktor_version"
            const val webjars = "io.ktor:ktor-server-webjars:$ktor_version"
            const val websockets = "io.ktor:ktor-server-websockets:$ktor_version"
            const val double_receive = "io.ktor:ktor-server-double-receive:$ktor_version"

            fun full(scope: DependencyHandlerScope) = with(scope) {
                implementation(auth)
                implementation(auth_jwt)
                implementation(auto_head)
                implementation(caching_headers)
                implementation(content_negotiation)
                implementation(compression)
                implementation(core)
                implementation(cors)
                implementation(default_headers)
                implementation(hsts)
                implementation(html_builder)
                implementation(host_common)
                implementation(netty)
                implementation(metrics)
                implementation(partial_content)
                implementation(sessions)
                implementation(sse)
                implementation(status_pages)
                implementation(webjars)
                implementation(websockets)
                implementation(double_receive)
            }
        }

        object Client {
            const val core = "io.ktor:ktor-client-core:$ktor_version"

            const val apache = "io.ktor:ktor-client-apache:$ktor_version"
            const val cio = "io.ktor:ktor-client-cio:$ktor_version"
            const val okhttp = "io.ktor:ktor-client-okhttp:$ktor_version"

            const val content_negotiation = "io.ktor:ktor-client-content-negotiation:$ktor_version"

            const val sse = "io.ktor:ktor-client-sse:$ktor_version"
            const val plugins = "io.ktor:ktor-client-plugins:$ktor_version"
            const val logging = "io.ktor:ktor-client-logging:$ktor_version"
            const val websockets = "io.ktor:ktor-client-websockets:$ktor_version"
        }

        object Common {
            const val serialization_jackson = "io.ktor:ktor-serialization-jackson:$ktor_version"
            const val serialization_kotlinx_json = "io.ktor:ktor-serialization-kotlinx-json:$ktor_version"
            const val serialization_kotlinx_xml = "io.ktor:ktor-serialization-kotlinx-xml:$ktor_version"
            const val serialization_kotlinx_cbor = "io.ktor:ktor-serialization-kotlinx-cbor:$ktor_version"
            const val serialization_kotlinx_protobuf = "io.ktor:ktor-serialization-kotlinx-protobuf:$ktor_version"

        }
    }

    object JavaLibs {
        object ArangoDb {
            // https://mvnrepository.com/artifact/com.arangodb/arangodb-java-driver
            private const val driver_version = "7.17.0"
            const val java_driver = "com.arangodb:arangodb-java-driver:$driver_version"
        }

        object Jackson {
            // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
            private const val jackson_version = "2.18.3"

            // https://mvnrepository.com/artifact/com.fasterxml.jackson.module/jackson-module-kotlin
            private const val jackson_kotlin_module_version = "2.18.3"

            const val databind = "com.fasterxml.jackson.core:jackson-databind:$jackson_version"
            const val annotations = "com.fasterxml.jackson.core:jackson-annotations:$jackson_version"
            const val datatype_jdk8 = "com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jackson_version"
            const val datatype_jsr310 = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jackson_version"

            const val module_kotlin =
                "com.fasterxml.jackson.module:jackson-module-kotlin:$jackson_kotlin_module_version"

            fun fullImpl(scope: DependencyHandlerScope) = with(scope) {
                implementation(databind)
                implementation(annotations)
                implementation(datatype_jdk8)
                implementation(datatype_jsr310)
                implementation(module_kotlin)
            }
        }

        object Aws {
            // https://mvnrepository.com/artifact/software.amazon.awssdk/s3
            const val awssdk_version = "2.25.26"

            const val s3 = "software.amazon.awssdk:s3:$awssdk_version"
            const val ses = "software.amazon.awssdk:ses:$awssdk_version"
        }

        object Stripe {
            // TODO: upgrade at some point BUT this also means an API-Level update ...
            // https://mvnrepository.com/artifact/com.stripe/stripe-java
            private const val sdk_version = "21.15.0"
            const val java_sdk = "com.stripe:stripe-java:$sdk_version"
        }

        object Google {
            // https://mvnrepository.com/artifact/com.google.auto.service/auto-service
            private const val auto_service_version = "1.1.1"
            const val auto_service = "com.google.auto.service:auto-service:$auto_service_version"

            // https://mvnrepository.com/artifact/com.google.api-client/google-api-client
            private const val api_client_version = "2.7.2"
            const val api_client = "com.google.api-client:google-api-client:$api_client_version"

            // https://mvnrepository.com/artifact/com.google.firebase/firebase-admin
            private const val firebase_admin_version = "9.2.0"
            const val firebase_admin = "com.google.firebase:firebase-admin:$firebase_admin_version"
        }

        object Pdf {
            // https://mvnrepository.com/artifact/org.xhtmlrenderer/flying-saucer-core
            private const val flying_saucer_version = "9.7.2"
            const val flying_saucer_core = "org.xhtmlrenderer:flying-saucer-core:$flying_saucer_version"

            // https://mvnrepository.com/artifact/org.xhtmlrenderer/flying-saucer-pdf-itext5
            private const val flying_saucer_itext5_version = "9.7.2"
            const val flying_saucer_itext = "org.xhtmlrenderer:flying-saucer-pdf-itext5:$flying_saucer_itext5_version"

            // https://mvnrepository.com/artifact/org.apache.pdfbox/pdfbox
            private const val apache_pdfbox_version = "3.0.4"
            const val apache_pdfbox = "org.apache.pdfbox:pdfbox:$apache_pdfbox_version"

            fun full(scope: DependencyHandlerScope) = with(scope) {
                implementation(flying_saucer_core)
                implementation(flying_saucer_itext)
                implementation(apache_pdfbox)
            }
        }

        object Moshi {
            // https://mvnrepository.com/artifact/com.squareup.moshi/moshi-kotlin
            private const val moshi_version = "1.15.2"
            const val kotlin = "com.squareup.moshi:moshi-kotlin:$moshi_version"
            const val adapters = "com.squareup.moshi:moshi-adapters:$moshi_version"
        }

        object ApacheCommons {
            // https://mvnrepository.com/artifact/org.apache.commons/commons-email
            private const val email_version = "1.6.0"
            const val email = "org.apache.commons:commons-email:$email_version"

            // https://mvnrepository.com/artifact/commons-cli/commons-cli
            private const val cli_version = "1.6.0"
            const val cli = "commons-cli:commons-cli:$cli_version"
        }

        // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
        private const val logback_version = "1.5.17"
        const val logback_classic = "ch.qos.logback:logback-classic:$logback_version"

        // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
        private const val slf4j_version = "2.0.17"
        const val slf4j_api = "org.slf4j:slf4j-api:$slf4j_version"

        // https://mvnrepository.com/artifact/org.commonmark/commonmark
        private const val commonmark_version = "0.24.0"
        const val commonmark = "org.commonmark:commonmark:$commonmark_version"
        const val commonmark_ext_gfm_tables = "org.commonmark:commonmark-ext-gfm-tables:$commonmark_version"

        // https://mvnrepository.com/artifact/com.talanlabs/avatar-generator
        private const val avatar_generator_version = "1.1.0"
        const val avatar_generator = "com.talanlabs:avatar-generator:$avatar_generator_version"
        const val avatar_generator_smiley = "com.talanlabs:avatar-generator-smiley:$avatar_generator_version"
        const val avatar_generator_8bit = "com.talanlabs:avatar-generator-8bit:$avatar_generator_version"

        // https://mvnrepository.com/artifact/com.notnoop.apns/apns
        // Java Apple Push Notification Service Library
        private const val apns_version = "1.0.0.Beta6"
        const val apns = "com.notnoop.apns:apns:$apns_version"

        // https://mvnrepository.com/artifact/org.apache.tika/tika-core
        // ... mime type detector based on content of file
        private const val tika_version = "3.1.0"
        const val tika_core = "org.apache.tika:tika-core:$tika_version"

        // https://search.maven.org/artifact/net.iakovlev/timeshape/2024a.25/jar?eh=
        private const val timeshape_version = "2024a.25"
        const val timeshape = "net.iakovlev:timeshape:$timeshape_version"

        // https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp
        private const val okhttp_version = "4.12.0"
        const val okhttp = "com.squareup.okhttp3:okhttp:$okhttp_version"

        // https://mvnrepository.com/artifact/com.github.wumpz/diffutils
        private const val diff_utils_version = "3.0"
        const val diff_utils = "com.github.wumpz:diffutils:$diff_utils_version"
    }

    object IDE {
        const val jetbrains_annotations_version = "26.0.2"
        const val jetbrains_annotations = "org.jetbrains:annotations:$jetbrains_annotations_version"
    }

    object WebJars {
        // jQuery: https://www.webjars.org/
        private const val jquery_version = "3.7.1"
        const val jquery = "org.webjars:jquery:$jquery_version"

        // Lazy Sized: https://www.webjars.org/
        private const val lazysizes_version = "5.1.2"
        const val lazysizes = "org.webjars.bower:lazysizes:$lazysizes_version"

        // VisJS: https://www.webjars.org/
        private const val visjs_version = "4.21.0"
        const val visjs = "org.webjars:visjs:$visjs_version"

        // https://mvnrepository.com/artifact/org.webjars.bower/slick-carousel
        private const val slick_carousel_version = "1.8.1"
        const val slick_carousel = "org.webjars.bower:slick-carousel:$slick_carousel_version"

        // https://mvnrepository.com/artifact/org.webjars.npm/glidejs__glide
        private const val glidejs_version = "3.4.1"
        const val glidejs = "org.webjars.npm:glidejs__glide:$glidejs_version"

        // https://mvnrepository.com/artifact/org.webjars.npm/prismjs
        private const val prismjs_version = "1.29.0"
        const val prismjs = "org.webjars.npm:prismjs:$prismjs_version"

        // https://www.webjars.org/
        private const val fomanticui_version = "2.8.8"
        const val fomanticui_css = "org.webjars.npm:fomantic-ui-css:$fomanticui_version"
        const val fomanticui_js = "org.webjars.npm:fomantic-ui-js:$fomanticui_version"
    }

    // // NPM dependencies /////////////////////////////////////////////////////////////////////////

    object Npm {
        operator fun <T> invoke(block: Npm.() -> T): T {
            return this.block()
        }

        fun KotlinDependencyHandler.katex() = npm("katex", "0.16.21")
        fun KotlinDependencyHandler.markedKatexExtension() = npm("marked-katex-extension", "5.1.4")

    }

    // // Test dependencies ////////////////////////////////////////////////////////////////////////

    object Test {

        operator fun invoke(block: Test.() -> Unit) {
            this.block()
        }

        // https://mvnrepository.com/artifact/ch.qos.logback/logback-classic
        const val logback_version = "1.5.17"
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

    private fun DependencyHandlerScope.testImplementation(dep: String) =
        add("testImplementation", dep)

    private fun DependencyHandlerScope.api(dep: String) =
        add("api", dep)

    private fun DependencyHandlerScope.implementation(dep: String) =
        add("implementation", dep)

}
