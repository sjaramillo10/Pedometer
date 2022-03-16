plugins {
    // There is a "false-positive" error highlighted below. More info: https://youtrack.jetbrains.com/issue/KTIJ-19369
    alias(libs.plugins.ktlint)
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.android.gradle)
        classpath(libs.hilt.gradle)
        classpath(libs.kotlin.gradle)
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    repositories {
        mavenCentral()
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
