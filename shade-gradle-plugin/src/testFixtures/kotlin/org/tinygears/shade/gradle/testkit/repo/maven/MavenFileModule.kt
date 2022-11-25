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

open class MavenFileModule constructor(moduleDir: TestFile,
                                       groupId:    String,
                                       artifactId: String,
                                       version:    String): AbstractMavenModule(moduleDir, groupId, artifactId, version) {

    override var uniqueSnapshots = true

    override fun withNonUniqueSnapshots(): MavenModule {
        this.uniqueSnapshots = false
        return this
    }

    override fun getMetaDataFileContent(): String {
        return """
        <metadata>
        <!-- ${getArtifactContent()} -->
            <groupId>$groupId</groupId>
            <artifactId>$artifactId</artifactId>
            <version>$version</version>
            <versioning>
                <snapshot>
                    <timestamp>${timestampFormat.format(publishTimestamp)}</timestamp>
                    <buildNumber>$publishCount</buildNumber>
                </snapshot>
                <lastUpdated>${updateFormat.format(publishTimestamp)}</lastUpdated>
            </versioning>
        </metadata>
        """.trimIndent()
    }

    override fun onPublish(file: TestFile) {
        sha1File(file)
        md5File(file)
    }

    override fun publishesMetaDataFile(): Boolean {
        return uniqueSnapshots && version.endsWith("-SNAPSHOT")
    }

    override fun publishesHashFiles(): Boolean {
        return true
    }
}
