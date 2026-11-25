import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    alias(libs.plugins.metro)
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.sjaramillo.nemi"
    compileSdk {
        version =
            release(37) {
                minorApiLevel = 2
            }
    }

    defaultConfig {
        applicationId = "dev.sjaramillo.nemi"
        targetSdk = 37
        minSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // https://developer.android.com/jetpack/androidx/releases/room#compiler-options
        javaCompileOptions {
            annotationProcessorOptions {
                arguments +=
                    mapOf(
                        "room.schemaLocation" to "$projectDir/schemas",
                        "room.incremental" to "true",
                        "room.expandProjection" to "true",
                    )
            }
        }
    }

    androidResources {
        localeFilters += "en"
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    signingConfigs {
        val props = Properties()
        props.load(FileInputStream(project.file("signing/debug.properties")))

        getByName("debug") {
            storeFile = file("signing/debug.jks")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
            storePassword = props.getProperty("storePassword")
        }

        // TODO Add release signing configs
    }
}

dependencies {
    implementation(libs.activity.compose)
    implementation(libs.compose.foundation)
    debugImplementation(libs.compose.ui.tooling)

    // Health Connect
    implementation(libs.health.connect.client)

    // Room
    implementation(libs.room.core)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // ViewModel
    implementation(libs.viewmodel.core)
    implementation(libs.lifecycle.compose)

    // Unit Tests
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.sqlite.jdbc)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}
