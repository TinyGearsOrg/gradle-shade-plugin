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
package org.tinygears.shade.gradle.shrinking

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.InputFiles
import java.io.File

import java.nio.file.Path
import java.util.LinkedList

/**
 * A base class for implementations that can track unused classes in the project classpath.
 */
abstract class UnusedTracker protected constructor(toMinimize: FileCollection) {

    @InputFiles
    protected val toMinimize: FileCollection = toMinimize

    open fun getPathToProcessedClass(classname: String): Path? {
        return null
    }

    abstract fun findUnused(): Set<String>

    abstract fun addDependency(jarOrDir: File)

    companion object {
        private fun isProjectDependencyFile(file: File, dep: Dependency): Boolean {
            val fileName       = file.name
            val dependencyName = dep.name

            return (fileName == "${dependencyName}.jar") ||
                   (fileName.startsWith("${dependencyName}-") && fileName.endsWith(".jar"))
        }

        private fun addJar(config: Configuration, dep: Dependency, result: MutableList<File>) {
            val file = config.find { isProjectDependencyFile(it, dep) }
            if (file != null) {
                result.add(file)
            }
        }

        fun getApiJarsFromProject(project: Project): FileCollection {
            val apiDependencies      = project.configurations.asMap["api"]?.dependencies ?: return project.files()
            val runtimeConfiguration = project.configurations.asMap["runtimeClasspath"] ?: return project.files()

            val apiJars = LinkedList<File>()
            apiDependencies.forEach { dep ->
                when (dep) {
                    is ProjectDependency -> {
                        apiJars.addAll(getApiJarsFromProject(dep.dependencyProject))
                        addJar(runtimeConfiguration, dep, apiJars)
                    }

                    is SelfResolvingDependency -> {
                        apiJars.addAll(dep.resolve())
                    }

                    else -> {
                        addJar(runtimeConfiguration, dep, apiJars)
                        apiJars.add(runtimeConfiguration.find { it.name.startsWith("${dep.name}-") } as File)
                    }
                }
            }

            return project.files(apiJars)
        }
    }
}
