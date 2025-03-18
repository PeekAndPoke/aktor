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
                implementation(Deps.KotlinLibs.Ultra.common)
            }
        }

        commonTest {
            dependencies {
                Deps.Test { commonTestDeps() }
            }
        }

        jsMain {
            dependencies {
            }
        }

        jsTest {
            dependencies {
                Deps.Test { jsTestDeps() }
            }
        }

        jvmMain {
            dependencies {
                implementation(Deps.KotlinLibs.Ultra.kontainer)
                implementation(Deps.JavaLibs.timeshape)
            }
        }

        jvmTest {
            dependencies {}
        }
    }
}

