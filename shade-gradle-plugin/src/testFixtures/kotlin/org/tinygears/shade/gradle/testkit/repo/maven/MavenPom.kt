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
package org.tinygears.shade.gradle.testkit.repo.maven

import groovy.util.Node
import groovy.util.NodeList
import groovy.xml.XmlParser
import java.io.File

class MavenPom constructor(val groupId:     String?,
                           val artifactId:  String?,
                           val version:     String?,
                           val packaging:   String?,
                           val description: String?,
                           val scopes:      MutableMap<String, MavenScope> = mutableMapOf()) {

    companion object {
        fun from(pomFile: File): MavenPom {
            if (pomFile.exists()) {
                val pom = XmlParser().parse(pomFile)

                val groupId     = pom.getNode("groupId")?.text()
                val artifactId  = pom.getNode("artifactId")?.text()
                val version     = pom.getNode("version")?.text()
                val packaging   = pom.getNode("packaging")?.text()
                val description = pom.getNode("description")?.text()
                val scopes      = mutableMapOf<String, MavenScope>()

                val dependencies = pom.getNodeList("dependencies")
                for (dep in dependencies!!) {
                    val depNode = dep as Node
                    val scopeElement = depNode.getNode("scope")
                    val scopeName = if (scopeElement != null) scopeElement.text() else "runtime"
                    val scope = scopes.computeIfAbsent(scopeName) { MavenScope() }

                    val mavenDependency =
                        MavenDependency(depNode.getNode("groupId")?.text(),
                                        depNode.getNode("artifactId")?.text(),
                                        depNode.getNode("version")?.text(),
                                        depNode.getNode("classifier")?.text(),
                                        depNode.getNode("type")?.text())

                    var key = "${mavenDependency.groupId}:${mavenDependency.artifactId}:${mavenDependency.version}"
                    if (mavenDependency.classifier != null) {
                        key += mavenDependency.classifier
                    }

                    scope.dependencies[key] = mavenDependency
                }

                return MavenPom(groupId, artifactId, version, packaging, description, scopes)
            } else {
                error("pom file $pomFile does not exist")
            }
        }
    }
}

internal fun Node.getNode(name: String): Node? {
    val r = get(name)
    return if (r == null) {
        r
    } else {
        r as Node
    }
}

internal fun Node.getNodeList(name: String): NodeList? {
    val r = get(name)
    return if (r == null) {
        r
    } else {
        r as NodeList
    }
}