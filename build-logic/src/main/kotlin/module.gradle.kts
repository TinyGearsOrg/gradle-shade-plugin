import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = Versions.JVM_TARGET
        freeCompilerArgs = freeCompilerArgs + listOf("-Xjvm-default=all", "-Xlambdas=indy")
    }
}

java {
    sourceCompatibility = JavaVersion.toVersion(Versions.JVM_TARGET)
    targetCompatibility = JavaVersion.toVersion(Versions.JVM_TARGET)
}
