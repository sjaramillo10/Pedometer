plugins {
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.compose) apply false
}

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(libs.android.gradle)
        classpath(libs.hilt.gradle)
    }
}

subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
