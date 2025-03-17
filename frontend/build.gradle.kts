@file:Suppress("PropertyName")

import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig


plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    idea
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

// Configure the Yarn plugin extension to use a custom lock file
//rootProject.plugins.withType<YarnPlugin> {
//    rootProject.extensions.getByType<YarnRootExtension>().lockFileName = "${project.name}.yarn.lock"
//}

kotlin {
    js {
        browser {
            binaries.executable()

            // Add webpack configuration
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).copy(
                    port = 25867
                )
            }
        }
    }

    jvmToolchain(Deps.jvmTargetVersion)

    jvm {
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(Deps.KotlinX.serialization_core)
                implementation(Deps.KotlinX.serialization_json)

                implementation(Deps.KotlinLibs.uuid)
                implementation(Deps.KotlinLibs.Kraft.core)
            }
        }

        commonTest {
            dependencies {
                Deps.Test { commonTestDeps() }
            }
        }

        jsMain {
            dependencies {
                implementation(Deps.KotlinX.coroutines_core)

                implementation(Deps.Ktor.Client.core)
                implementation(Deps.Ktor.Client.content_negotiation)
                implementation(Deps.Ktor.Common.serialization_kotlinx_json)

                implementation(Deps.KotlinLibs.Kraft.addons_marked)
                implementation(Deps.KotlinLibs.Kraft.addons_pdfjs)
                implementation(Deps.KotlinLibs.Kraft.addons_signaturepad)
                implementation(Deps.KotlinLibs.Kraft.addons_sourcemappedstacktrace)
            }
        }

        jsTest {
            dependencies {
                Deps.Test { jsTestDeps() }
            }
        }
    }
}

