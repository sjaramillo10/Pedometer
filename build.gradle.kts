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

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
