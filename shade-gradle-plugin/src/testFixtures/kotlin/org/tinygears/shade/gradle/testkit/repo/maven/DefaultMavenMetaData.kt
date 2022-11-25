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

import groovy.xml.XmlParser
import java.io.File

/**
 * http://maven.apache.org/ref/3.0.1/maven-repository-metadata/repository-metadata.html
 */
class DefaultMavenMetaData constructor(         val text:        String,
                                                val groupId:     String?,
                                                val artifactId:  String?,
                                                val version:     String?,
                                       override val versions:    MutableList<String> = mutableListOf(),
                                                val lastUpdated: String?): MavenMetaData {

    companion object {
        fun from(file: File): DefaultMavenMetaData {
            val text = file.bufferedReader().readText()
            val xml = XmlParser().parseText(text)

            val groupId    = xml.getNode("groupId")?.text()
            val artifactId = xml.getNode("artifactId")?.text()
            val version    = xml.getNode("version")?.text()

            val versions   = mutableListOf<String>()
            val versioning = xml.getNodeList("versioning")!!

            // TODO
//            val lastUpdated = versioning.[0]?.text()
//
//            versioning.versions[0].version.each {
//                versions << it.text()
//            }

            return DefaultMavenMetaData(text, groupId, artifactId, version, versions, null)
        }
    }
}
