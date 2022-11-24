import org.tinygears.shade.gradle.tasks.ShadeJar
import org.tinygears.shade.gradle.transformation.ApacheLicenseResourceTransformer

plugins {
    kotlin("jvm") version "1.7.20"
    id("org.tinygears.shade")
}

tasks {
    named<ShadeJar>("shadeJar") {
        archiveBaseName.set("sample")
        // mergeServiceFiles()
        manifest {
            attributes(mapOf("Main-Class" to "org.tinygears.shade.sample.ExampleApp"))
        }

        minimize()
        useR8()

        relocate("com.google", "shadow.com.google")
    }
}

dependencies {
    implementation("com.google.guava:guava:31.1-jre")
    provided(libs.ant)
    testImplementation(libs.junit)
}
