check(JavaVersion.current().isCompatibleWith(JavaVersion.VERSION_25)) {
    "Nemi requires JDK 25 or newer to run Gradle. Found Java ${JavaVersion.current()}."
}

rootProject.name = "Nemi"

include(":app")
