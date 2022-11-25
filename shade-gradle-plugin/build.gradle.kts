plugins {
    id("module")
    `java-gradle-plugin`
    `java-test-fixtures`
    id("idea")
}

group = "org.tinygears"
version = Versions.currentOrSnapshot()

val generateTestFixtureSources = task("generateTestFixtureSources") {
    inputs.property("version", project.version)
    outputs.dir("$buildDir/generated")

    doFirst {
        val versionFile = file("$buildDir/generated/org/tinygears/shade/gradle/Version.kt")
        versionFile.parentFile.mkdirs()
        versionFile.writeText(
            """
            |package org.tinygears.shade.gradle
            |
            |object Version {
	        |    val version: String
            |        get() = "${project.version}"
            |}
            """.trimMargin()
        )
    }
}

tasks.compileTestFixturesKotlin {
    dependsOn(generateTestFixtureSources)
}

sourceSets {
    testFixtures {
        kotlin {
            srcDir("$buildDir/generated")
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())

    implementation(libs.bat.common)
    implementation(libs.bat.classfile)

    implementation(libs.ant)
    implementation(libs.jdependency)
    implementation(libs.r8)

    testFixturesApi(libs.jupiter.api)
    testFixturesApi(libs.jupiter.params)
    testFixturesImplementation(libs.commons.lang)
    testFixturesImplementation(libs.commons.io)
    testFixturesImplementation(gradleTestKit())
}

gradlePlugin {
    plugins {
        create("org.tinygears.shade") {
            id = "org.tinygears.shade"
            implementationClass = "org.tinygears.shade.gradle.ShadePlugin"
            displayName = "Shading capabilities for java / kotlin libraries"
            description = "Shading capabilities for java / kotlin libraries"
        }
    }
}

//afterEvaluate {
//    publishing {
//        publications.filterIsInstance<MavenPublication>().forEach {
//            it.pom {
//                description.set("The official Detekt Gradle Plugin")
//                name.set("detekt-gradle-plugin")
//                url.set("https://detekt.dev")
//                licenses {
//                    license {
//                        name.set("The Apache Software License, Version 2.0")
//                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
//                        distribution.set("repo")
//                    }
//                }
//                developers {
//                    developer {
//                        id.set("Detekt Developers")
//                        name.set("Detekt Developers")
//                        email.set("info@detekt.dev")
//                    }
//                }
//                scm {
//                    url.set("https://github.com/detekt/detekt")
//                }
//            }
//        }
//    }
//}
