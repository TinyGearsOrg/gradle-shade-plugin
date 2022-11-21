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

import groovy.util.Node
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.artifacts.SelfResolvingDependency
import org.gradle.api.file.CopySpec
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.tinygears.shade.gradle.ShadePlugin.Companion.PROVIDED_CONFIGURATION_NAME

open class ShadeExtension constructor(private val project: Project) {

    val applicationDistribution: CopySpec = project.copySpec {}

    fun component(publication: MavenPublication) {
        publication.artifact(project.tasks.named(ShadePlugin.SHADE_TASK_NAME))

        publication.pom { pom: MavenPom ->
            pom.withXml { xml ->
                val dependenciesNode = (xml.asNode().get("dependencies") ?: xml.asNode().appendNode("dependencies")) as Node
                project.configurations.getByName(PROVIDED_CONFIGURATION_NAME).allDependencies.forEach {
                    if (it is ProjectDependency || it !is SelfResolvingDependency) {
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId",    it.group)
                        dependencyNode.appendNode("artifactId", it.name)
                        dependencyNode.appendNode("version",    it.version)
                        dependencyNode.appendNode("scope",      "runtime")
                    }
                }
            }
        }
    }
}
