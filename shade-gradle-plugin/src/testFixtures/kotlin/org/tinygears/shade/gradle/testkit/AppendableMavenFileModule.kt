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

import org.apache.commons.io.IOUtils
import org.tinygears.shade.gradle.testkit.file.TestFile
import org.tinygears.shade.gradle.testkit.repo.maven.MavenFileModule
import java.io.File
import java.io.OutputStream

class AppendableMavenFileModule constructor(moduleDir: TestFile,
                                            groupId:    String,
                                            artifactId: String,
                                            version:    String): MavenFileModule(moduleDir, groupId, artifactId, version) {

    private val contents: MutableMap<String, MutableMap<String, String>> = mutableMapOf()
    private val files: MutableMap<String, File> = mutableMapOf()

    fun use(file: File): AppendableMavenFileModule {
        return use("", file)
    }

    fun use(classifier: String, file: File): AppendableMavenFileModule {
        files[classifier] = file
        return this
    }

    fun insertFile(path: String, content: String): AppendableMavenFileModule {
        insertFile("", path, content)
        return this
    }

    fun insertFile(classifier: String, path: String, content: String): AppendableMavenFileModule {
        contents.computeIfAbsent(classifier) { mutableMapOf() }[path] = content
        return this
    }

    override fun publishArtifact(artifact: Map<String, String>): File {
        val artifactFile = artifactFile(artifact)
        if (type == "pom") {
            return artifactFile
        }
        val classifier = artifact["classifier"] ?: ""
        if (files.containsKey(classifier)) {
            publishWithStream(artifactFile) { os: OutputStream ->
                IOUtils.copy(files[classifier]?.inputStream(), os)
            }
        } else {
            publishWithStream(artifactFile) { os: OutputStream ->
                writeJar(os, contents[classifier])
            }
        }
        return artifactFile
    }

    fun writeJar(os: OutputStream, contents: Map<String, String>?) {
        if (contents != null) {
            val builder = JarBuilder(os)
            for ((path, content) in contents) {
                builder.withFile(path, content)
            }
            builder.build()
        }
    }

    /**
     * Add an artifact to this module.
     * @param options can specify any of: type or classifier
     */
    override fun artifact(options: Map<String, String>): AppendableMavenFileModule {
        artifacts.add(options)
        return this
    }
}
