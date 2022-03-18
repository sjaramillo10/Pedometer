import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("dagger.hilt.android.plugin")
    id("kotlin-android")
    id("kotlin-kapt") // TODO Migrate to KSP when stable
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "dev.sjaramillo.pedometer"
        targetSdk = 31
        minSdk = 21
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // https://developer.android.com/jetpack/androidx/releases/room#compiler-options
        javaCompileOptions {
            annotationProcessorOptions {
                arguments += mapOf(
                    "room.schemaLocation" to "$projectDir/schemas",
                    "room.incremental" to "true",
                    "room.expandProjection" to "true"
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

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.ui.get()
    }

    buildTypes {
        getByName("debug") {
            isMinifyEnabled = false
        }
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
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

    sourceSets.configureEach {
        // Workaround so that ktlint considers contents inside the kotlin folders by default
        // More info: https://github.com/JLLeitschuh/ktlint-gradle/issues/524#issuecomment-915639053
        java.srcDirs("src/$name/kotlin")
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
    implementation(libs.activity.compose)
    implementation(libs.compose.material.core)
    implementation(libs.compose.material.theme.adapter)
    implementation(libs.compose.ui.tooling)

    // Hilt
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)
    implementation(libs.hilt.core)
    kapt(libs.hilt.compiler)

    // Material Components
    implementation(libs.material)

    // Navigation
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)

    // Room
    implementation(libs.room.core)
    implementation(libs.room.runtime)
    kapt(libs.room.compiler)

    // ViewModel
    implementation(libs.viewmodel.compose)
    implementation(libs.viewmodel.core)

    // WorkManager
    implementation(libs.work.runtime)

    // Other
    implementation(libs.logcat)
    implementation(libs.eaze.graph)

    // Unit Tests
    testImplementation(libs.junit)

    // Instrumentation tests
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.hilt.testing)
    androidTestImplementation(libs.compose.ui.test.junit)
    androidTestImplementation(libs.core.testing)
    androidTestImplementation(libs.work.testing)
    kaptAndroidTest(libs.hilt.compiler)
}

kapt {
    correctErrorTypes = true
}
