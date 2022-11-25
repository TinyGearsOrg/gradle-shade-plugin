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

import org.tinygears.shade.gradle.testkit.file.TestFile

interface MavenModule {
    val pomFile: TestFile
    val artifactFile: TestFile
    val metaDataFile: TestFile
    val parsedPom: MavenPom
    val rootMetaData: MavenMetaData

    /**
     * Publishes the pom.xml plus main artifact, plus any additional artifacts for this module. Publishes only those artifacts whose content has
     * changed since the last call to {@code #publish()}.
     */
    fun publish(): MavenModule

    /**
     * Publishes the pom.xml only
     */
    fun publishPom(): MavenModule

    /**
     * Publishes the pom.xml plus main artifact, plus any additional artifacts for this module, with different content (and size) to any
     * previous publication.
     */
    fun publishWithChangedContent(): MavenModule

    fun withNonUniqueSnapshots(): MavenModule

    fun parent(group: String, artifactId: String, version: String): MavenModule

    fun dependsOn(group: String, artifactId: String, version: String): MavenModule

    fun hasPackaging(packaging: String): MavenModule

    /**
     * Sets the type of the main artifact for this module.
     */
    fun hasType(type: String): MavenModule
}
