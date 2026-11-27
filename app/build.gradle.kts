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

    signingConfigs {
        getByName("debug") {
            storeFile = file("debug.jks")
            keyAlias = "android"
        }
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
}

dependencies {
    testImplementation(libs.junit)
}

tasks.register("printBuildToolsDir") {
    val sdk = androidComponents.sdkComponents.sdkDirectory
    val tools = sdk.get().dir("build-tools/${android.buildToolsVersion}").asFile
    doLast { println(tools.absolutePath) }
}
