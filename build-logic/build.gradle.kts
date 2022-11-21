plugins {
    `kotlin-dsl`
}

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    implementation(libs.kotlin.gradle)
}
