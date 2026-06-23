plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.android.library)
    alias(libs.plugins.sqldelight)
}

kotlin {
    compilerOptions {
        // expect/actual classes are in Beta — suppress the warning project-wide
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
                }
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { target ->
        target.binaries.framework {
            baseName = "shared"
            isStatic = false
        }
    }

    sourceSets {
        commonMain.dependencies {
            // Compose Multiplatform
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.components.resources)

            // Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
            implementation(libs.ktor.client.auth)

            // DI
            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            // Async
            implementation(libs.kotlinx.coroutines.core)

            // Data
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)

            // Local storage
            implementation(libs.multiplatform.settings.no.arg)

            // Image loading (avatar display)
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)

            // Database
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.play.services.location)   // FusedLocation + Activity Recognition
            implementation(libs.sqldelight.driver.android)
            implementation(libs.androidx.credentials)                     // Credential Manager
            implementation(libs.androidx.credentials.play.services.auth) // Google Sign-In backcompat
            implementation(libs.googleid)                                // Google ID token helper
            implementation(libs.androidx.security.crypto)                // EncryptedSharedPreferences for auth tokens
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
            implementation(libs.sqldelight.driver.native)
        }
    }
}

android {
    namespace = "dev.atmos.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

sqldelight {
    databases {
        create("AtmosDatabase") {
            packageName.set("dev.atmos.shared.db")
            // generateAsync not set — Android + iOS use synchronous drivers.
            // Flow wrappers come from coroutines-extensions via .asFlow() at query call sites.
        }
    }
}
