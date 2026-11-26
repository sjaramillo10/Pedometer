import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

android {
    enableKotlin = false
    namespace = "dev.sjaramillo.nemi"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.sjaramillo.nemi"
        targetSdk = 37
        minSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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
    testImplementation(libs.junit)
}
