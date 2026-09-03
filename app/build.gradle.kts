import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "dev.sjaramillo.pedometer"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.sjaramillo.pedometer"
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

    buildFeatures {
        compose = true
    }

    compileOptions {
        // Flag to enable support for the new language APIs
        isCoreLibraryDesugaringEnabled = true
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
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
    // Enable Java 8+ API desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")

    // AndroidX
    implementation(libs.appcompat)
    implementation(libs.activity.core)
    implementation(libs.fragment)
    implementation(libs.preference)

    // Compose
    implementation(libs.bundles.compose)

    // Hilt
    implementation(libs.hilt.core)
    ksp(libs.hilt.compiler)

    // Health Connect
    implementation(libs.health.connect.client)

    // Material Components
    implementation(libs.material)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Room
    implementation(libs.room.core)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // ViewModel
    implementation(libs.viewmodel.compose)
    implementation(libs.viewmodel.core)
    implementation(libs.lifecycle.runtime)

    // Other
    implementation(libs.logcat)
    implementation(libs.eaze.graph)

    // Unit Tests
    testImplementation(libs.junit)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}
