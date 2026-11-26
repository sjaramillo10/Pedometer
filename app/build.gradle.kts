import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
}

android {
    namespace = "dev.sjaramillo.nemi"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.sjaramillo.nemi"
        targetSdk = 37
        minSdk = 34
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
    implementation(libs.activity.core) {
        // NavigationEvent uses Compose stability annotations. The View host does not use them.
        exclude(group = "androidx.compose.runtime", module = "runtime-annotation")
    }

    // Room
    implementation(libs.room.core)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)

    // ViewModel
    implementation(libs.viewmodel.core)
    implementation(libs.lifecycle.runtime)

    // Unit Tests
    testImplementation(libs.junit)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
    arg("room.expandProjection", "true")
}
