@file:Suppress("PropertyName")

import Deps.Test.configureJvmTests


plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.google.devtools.ksp") version Deps.Ksp.version
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

                implementation(Deps.KotlinLibs.Ultra.common)
                implementation(Deps.KotlinLibs.Ultra.security)
                implementation(Deps.KotlinLibs.Ultra.slumber)

                implementation(Deps.KotlinLibs.Kraft.core)

                implementation(Deps.KotlinLibs.Karango.addons)

                implementation(Deps.KotlinLibs.Funktor.rest)
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
                implementation(Deps.Ktor.Client.core)
                implementation(Deps.Ktor.Client.content_negotiation)
                implementation(Deps.Ktor.Common.serialization_kotlinx_json)

                implementation(Deps.KotlinLibs.Karango.core)
                implementation(Deps.KotlinLibs.Funktor.core)

                implementation(Deps.JavaLibs.Google.api_client)
            }
        }

        jvmTest {
            dependencies {
                Deps.Test { jvmTestDeps() }
            }
        }
    }
}

dependencies {
    add("kspJvm", Deps.KotlinLibs.Karango.ksp)
}

tasks {
    configureJvmTests()
}
