rootProject.name = "shade"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }

    includeBuild("build-logic")
    includeBuild("shade-gradle-plugin")
}

include("sample")

//include("shade-impl")
//include("shade-cli")

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
