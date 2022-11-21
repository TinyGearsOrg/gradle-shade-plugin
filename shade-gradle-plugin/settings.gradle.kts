rootProject.name = "shade-gradle-plugin"

pluginManagement {
    includeBuild("../build-logic")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
        google()
        maven { url = uri("https://jitpack.io") }
    }

    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
