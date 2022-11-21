plugins {
    id("module")
    `java-gradle-plugin`
    `java-test-fixtures`
}

group = "org.tinygears"
version = Versions.currentOrSnapshot()

dependencies {
    implementation(kotlin("stdlib"))
    implementation(gradleApi())

    implementation(libs.bat.common)
    implementation(libs.bat.classfile)

    implementation(libs.ant)
    implementation(libs.jdependency)
    implementation(libs.r8)
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
