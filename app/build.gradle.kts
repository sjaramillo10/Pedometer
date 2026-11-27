import com.android.build.api.dsl.ApkSigningConfig
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

fun ApkSigningConfig.loadCredentials(directory: File) {
    val propertiesFile = directory.resolve("signing.properties")
    val location = directory.relativeTo(rootDir)
    require(propertiesFile.isFile) { "Missing signing.properties in $location" }

    val properties = Properties()
    propertiesFile.inputStream().use { properties.load(it) }
    fun requiredProperty(name: String): String =
        requireNotNull(properties.getProperty(name)?.takeIf { it.isNotBlank() }) {
            "Missing $name in $location/signing.properties"
        }

    val keystore = directory.resolve(requiredProperty("storeFile"))
    require(keystore.isFile) { "Keystore missing in $location. Check storeFile in signing.properties." }
    storeFile = keystore
    storeType = requiredProperty("storeType")
    keyAlias = requiredProperty("keyAlias")
    keyPassword = requiredProperty("keyPassword")
    storePassword = requiredProperty("storePassword")
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

    val releaseSigningDirectory = rootProject.file(".signing/release")
    signingConfigs {
        getByName("debug") {
            loadCredentials(rootProject.file(".signing/debug"))
        }
        if (releaseSigningDirectory.exists()) {
            create("release") {
                loadCredentials(releaseSigningDirectory)
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    testImplementation(libs.junit)
}
