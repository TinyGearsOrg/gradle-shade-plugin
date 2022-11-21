/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.tinygears.shade.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.LibraryElements
import org.gradle.api.attributes.Usage
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Jar
import org.gradle.configuration.project.ProjectConfigurationActionContainer
import org.tinygears.shade.gradle.ShadePlugin.Companion.SHADE_GROUP
import org.tinygears.shade.gradle.ShadePlugin.Companion.SHADE_RUNTIME_ELEMENT
import org.tinygears.shade.gradle.ShadePlugin.Companion.SHADE_TASK_NAME
import org.tinygears.shade.gradle.tasks.ShadeJar
import javax.inject.Inject

class ShadeJavaPlugin
    @Inject constructor(private val configurationActionContainer: ProjectConfigurationActionContainer): Plugin<Project> {

    @Override
    override fun apply(project: Project) {
        configureShadowTask(project)

        project.configurations.named("compileClasspath") {
            it.extendsFrom(project.configurations.getByName(ShadePlugin.PROVIDED_CONFIGURATION_NAME))
        }

        val shadeRuntimeElements = project.configurations.create(SHADE_RUNTIME_ELEMENT).apply {
            isCanBeConsumed = true
            isCanBeResolved = false
            attributes.apply {
                attribute(Usage.USAGE_ATTRIBUTE, project.objects.named(Usage::class.java, Usage.JAVA_RUNTIME))
                attribute(Category.CATEGORY_ATTRIBUTE, project.objects.named(Category::class.java, Category.LIBRARY))
                attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, project.objects.named(LibraryElements::class.java, LibraryElements.JAR))
                attribute(Bundling.BUNDLING_ATTRIBUTE, project.objects.named(Bundling::class.java, Bundling.SHADOWED))
            }

            outgoing.artifact(project.tasks.named(SHADE_TASK_NAME))
        }

        shadeRuntimeElements.extendsFrom(project.configurations.getByName(ShadePlugin.PROVIDED_CONFIGURATION_NAME))

        project.components.named("java", AdhocComponentWithVariants::class.java) { component ->
            component.addVariantsFromConfiguration(shadeRuntimeElements) {
                it.mapToOptional() // make it a Maven optional dependency
            }
        }
    }

    private fun configureShadowTask(project: Project) {
        val convention: JavaPluginConvention = project.convention.getPlugin(JavaPluginConvention::class.java)
        project.tasks.register(SHADE_TASK_NAME, ShadeJar::class.java) { shade ->
            shade.group = SHADE_GROUP
            shade.description = "Create a combined JAR of project and runtime dependencies"
            shade.archiveClassifier.set("all")

            val jarTaskProvider = project.tasks.named("jar", Jar::class.java)
            shade.manifest.inheritFrom(jarTaskProvider.get().manifest)
            val libsProvider = project.provider { mutableListOf(jarTaskProvider.get().manifest.attributes["Class-Path"]) }
            val files = project.objects.fileCollection().from(
                project.configurations.findByName(ShadePlugin.PROVIDED_CONFIGURATION_NAME)
            )

            shade.doFirst {
                if (!files.isEmpty) {
                    val libs = libsProvider.get()
                    libs.addAll(files.map { file -> file.name })
                    shade.manifest.attributes(
                        mapOf<String, String>(
                            Pair("Class-Path",
                                libs.filterNotNull().joinToString(" ", transform = { it.toString() })
                            )
                        )
                    )
                }
            }

            shade.from(convention.sourceSets.getByName("main").output)
            shade.configurations = if (project.configurations.findByName("runtimeClasspath") != null) {
                mutableListOf(project.configurations.getByName("runtimeClasspath"))
            } else {
                mutableListOf(project.configurations.getByName("runtime"))
            }

            shade.exclude("META-INF/INDEX.LIST", "META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "module-info.class")

//            shade.dependencies {
//                it.exclude(it.dependency(project.dependencies.gradleApi()))
//            }
        }

        project.artifacts.add(ShadePlugin.PROVIDED_CONFIGURATION_NAME, project.tasks.named(SHADE_TASK_NAME))
    }
}

private fun Dependency.asString(): String {
    return "${group}:${name}:${version}"
}