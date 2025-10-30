@file:Suppress("PropertyName")


plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    idea
}

val GROUP: String by project
val VERSION_NAME: String by project

group = GROUP
version = VERSION_NAME

kotlin {
    js {
        browser {
            binaries.executable()
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
                implementation(Deps.KotlinLibs.Ultra.security)

                implementation(project(":libs:shared"))
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

                implementation(Deps.KotlinLibs.Funktor.auth)

                implementation(Deps.KotlinLibs.Kraft.semanticui)
                implementation(Deps.KotlinLibs.Kraft.addons_marked)
                implementation(Deps.KotlinLibs.Kraft.addons_pdfjs)
                implementation(Deps.KotlinLibs.Kraft.addons_signaturepad)
                implementation(Deps.KotlinLibs.Kraft.addons_sourcemappedstacktrace)

                // JS deps
                implementation(Deps.Npm { unified() })
                implementation(Deps.Npm { remarkParse() })
                implementation(Deps.Npm { remarkGfm() })
                implementation(Deps.Npm { remarkMath() })
                implementation(Deps.Npm { remarkRehype() })
                implementation(Deps.Npm { rehypeMermaid() })
                implementation(Deps.Npm { rehypeHighlight() })
                implementation(Deps.Npm { rehypeKatex() })
                implementation(Deps.Npm { rehypeSanitize() })
                implementation(Deps.Npm { rehypeStringify() })
            }
        }

        jsTest {
            dependencies {
                Deps.Test { jsTestDeps() }
            }
        }
    }
}

