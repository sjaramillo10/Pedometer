import com.android.build.api.dsl.ApkSigningConfig
import com.android.build.api.dsl.ApplicationExtension
import java.io.File
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

    signingConfigs {
        getByName("debug") {
            loadCredentials(rootProject.file(".signing/debug"))
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        getByName("release") {
            signingConfig = localReleaseSigning()
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    testImplementation(libs.junit)
}

fun ApplicationExtension.localReleaseSigning(): ApkSigningConfig? {
    val directory = rootProject.file(".signing/release")
    if (!directory.exists()) return null

    return runCatching {
        signingConfigs.create("release") { loadCredentials(directory) }
    }.getOrElse { error ->
        val message = "Invalid release signing configuration: ${error.message}"
        tasks.matching { it.name == "preReleaseBuild" }.configureEach {
            doFirst { throw GradleException(message) }
        }
        null
    }
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
