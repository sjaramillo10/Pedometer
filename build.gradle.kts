buildscript {
    dependencies {
        // Pin the Kotlin compiler used by AGP.
        classpath(libs.kotlin.gradle.plugin)
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.metro) apply false
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp) apply false
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
