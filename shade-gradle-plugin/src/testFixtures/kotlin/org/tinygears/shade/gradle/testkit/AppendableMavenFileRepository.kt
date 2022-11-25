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
package org.tinygears.shade.gradle.testkit

import org.tinygears.shade.gradle.testkit.file.TestFile
import org.tinygears.shade.gradle.testkit.repo.maven.MavenFileRepository

class AppendableMavenFileRepository constructor(rootDir: TestFile): MavenFileRepository(rootDir) {
    override fun module(groupId: String, artifactId: String, version: String): AppendableMavenFileModule {
        val artifactDir = rootDir.file("${groupId.replace('.', '/')}/$artifactId/$version")
        return AppendableMavenFileModule(artifactDir, groupId, artifactId, version)
    }
}
