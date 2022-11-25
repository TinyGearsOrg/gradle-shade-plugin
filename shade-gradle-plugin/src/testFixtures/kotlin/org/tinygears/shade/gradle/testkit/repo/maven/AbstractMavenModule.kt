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

import org.tinygears.shade.gradle.testkit.repo.AbstractModule
import org.tinygears.shade.gradle.testkit.file.TestFile
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

abstract class AbstractMavenModule constructor(val moduleDir: TestFile,
                                               val groupId:          String,
                                               val artifactId:       String,
                                               val version:          String,
                                               var type:             String? = "jar",
                                               var packaging:        String? = null,
                                               var parentPomSection: String? = null,
                                               var publishCount:     Int     = 1): AbstractModule(), MavenModule {

    protected val dependencies: MutableList<Map<String, String>> = mutableListOf()
    protected val artifacts: MutableList<Map<String, String>>    = mutableListOf()

    abstract val uniqueSnapshots: Boolean

    override val parsedPom: MavenPom
        get() = MavenPom.from(pomFile)

    override val artifactFile: TestFile
        get() = artifactFile(emptyMap())

    override val pomFile: TestFile
        get() = moduleDir.file("$artifactId-${publishArtifactVersion}.pom")

    override val rootMetaData: MavenMetaData
        get() = DefaultMavenMetaData.from(rootMetaDataFile)

    override val metaDataFile: TestFile
        get() = moduleDir.file(MAVEN_METADATA_FILE)

    private val rootMetaDataFile: TestFile
        get() = moduleDir.parentFile!!.file(MAVEN_METADATA_FILE)

    val publishTimestamp: Date
        get() = Date(updateFormat.parse("20100101120000").time + publishCount * 1000)

    val publishArtifactVersion: String
        get() {
            if (uniqueSnapshots && version.endsWith("-SNAPSHOT")) {
                return "${version.replaceFirst("-SNAPSHOT", "")}-$uniqueSnapshotVersion"
            }
            return version
        }

    override fun parent(group: String, artifactId: String, version: String): MavenModule {
        parentPomSection =
            """
            <parent>
                <groupId>${group}</groupId>
                <artifactId>${artifactId}</artifactId>
                <version>${version}</version>
            </parent>
            """.trimIndent()
        return this
    }

    fun artifactFile(options: Map<String, String>): TestFile {
        val artifact = toArtifact(options)
        var fileName = "$artifactId-${publishArtifactVersion}.${artifact["type"]}"
        if (artifact["classifier"] != null) {
            fileName = "$artifactId-$publishArtifactVersion-${artifact["classifier"]}.${artifact["type"]}"
        }
        return moduleDir.file(fileName)
    }

    private fun toArtifact(options: Map<String, String>): Map<String, String> {
        val artifact = mutableMapOf<String, String>()
        val type = options.getOrDefault("type", type)
        if (type != null) {
            artifact["type"] = type
        }

        val classifier = options.getOrDefault("classifier", null)
        if (classifier != null) {
            artifact["classifier"] = classifier
        }

        return artifact
    }

    private val uniqueSnapshotVersion: String
        get() {
            assert(uniqueSnapshots && version.endsWith("-SNAPSHOT"))
            if (metaDataFile.isFile) {
//                val metaData = XmlParser().parse(metaDataFile.assertIsFile())
//                val timestamp = metaData.versioning.snapshot.timestamp[0].text().trim()
//                val build = metaData.versioning.snapshot.buildNumber[0].text().trim()
//                return "${timestamp}-${build}"
                return ""
            }
            return "${timestampFormat.format(publishTimestamp)}-${publishCount}"
        }

    fun dependsOn(vararg dependencyArtifactIds: String): MavenModule {
        for (id in dependencyArtifactIds) {
            dependsOn(groupId, id, "1.0")
        }
        return this
    }

    override fun dependsOn(group: String, artifactId: String, version: String): MavenModule {
        return dependsOn(group, artifactId, version, null)
    }

    fun dependsOn(group: String, artifactId: String, version: String, type: String?): MavenModule {
        val map = mutableMapOf<String, String>()

        map["groupId"] = group
        map["artifactId"] = artifactId
        map["version"] = version

        if (type != null) {
            map["type"] = type
        }

        this.dependencies.add(map)
        return this
    }

    override fun hasPackaging(packaging: String): MavenModule {
        this.packaging = packaging
        return this
    }

    /**
     * Specifies the type of the main artifact.
     */
    override fun hasType(type: String): MavenModule {
        this.type = type
        return this
    }

    /**
     * Add an artifact to this module.
     * @param options Can specify any of: type or classifier
     */
    open fun artifact(options: Map<String, String>): MavenModule {
        artifacts.add(options)
        return this
    }

    fun assertNotPublished() {
        pomFile.assertDoesNotExist()
    }

    fun assertPublished() {
        pomFile.assertExists()
        assert(parsedPom.groupId == groupId)
        assert(parsedPom.artifactId == artifactId)
        assert(parsedPom.version == version)
    }

    fun assertPublishedAsPomModule() {
        assertPublished()
        assertArtifactsPublished("${artifactId}-${publishArtifactVersion}.pom")
        assert(parsedPom.packaging == "pom")
    }

    fun assertPublishedAsJavaModule() {
        assertPublished()
        assertArtifactsPublished("${artifactId}-${publishArtifactVersion}.jar", "${artifactId}-${publishArtifactVersion}.pom")
        assert(parsedPom.packaging == null)
    }

    fun assertPublishedAsWebModule() {
        assertPublished()
        assertArtifactsPublished("${artifactId}-${publishArtifactVersion}.war", "${artifactId}-${publishArtifactVersion}.pom")
        assert(parsedPom.packaging == "war")
    }

    fun assertPublishedAsEarModule() {
        assertPublished()
        assertArtifactsPublished("${artifactId}-${publishArtifactVersion}.ear", "${artifactId}-${publishArtifactVersion}.pom")
        assert(parsedPom.packaging == "ear")
    }

    /**
     * Asserts that exactly the given artifacts have been deployed, along with their checksum files
     */
    fun assertArtifactsPublished(vararg names: String) {
//        val artifactNames = names as Set
//        if (publishesMetaDataFile()) {
//            artifactNames.add(MAVEN_METADATA_FILE)
//        }
//        assert(moduleDir.isDirectory())
//        Set actual = moduleDir.list() as Set
//        for (name in artifactNames) {
//            assert actual.remove(name)
//
//            if(publishesHashFiles()) {
//                assert actual.remove("${name}.md5" as String)
//                assert actual.remove("${name}.sha1" as String)
//            }
//        }
//        assert(actual.isEmpty())
    }

    override fun publishWithChangedContent(): MavenModule {
        publishCount++
        return publish()
    }

    override fun publishPom(): MavenModule {
        moduleDir.createDir()
        updateRootMavenMetaData(rootMetaDataFile)

        if (publishesMetaDataFile()) {
            publish(metaDataFile) { writer -> writer.appendLine(getMetaDataFileContent()) }
        }

        publish(pomFile) { writer ->
            val pomPackaging = packaging ?: type
            val text =
                """
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                <!-- ${getArtifactContent()} -->
                <modelVersion>4.0.0</modelVersion>
                <groupId>$groupId</groupId>
                <artifactId>$artifactId</artifactId>
                <packaging>$pomPackaging</packaging>
                <version>$version</version>
                <description>Published on $publishTimestamp</description>
                """.trimIndent()

            writer.appendLine(text)

            if (parentPomSection != null) {
                writer.appendLine()
                writer.append(parentPomSection)
                writer.appendLine()
            }

            if (dependencies.isNotEmpty()) {
                writer.appendLine("<dependencies>")
            }

            for (dependency in dependencies) {
                val typeAttribute = if (dependency["type"] == null) "" else "<type>$dependency.type</type>"
                val text =
                    """
                    <dependency>
                    <groupId>${dependency["groupId"]}</groupId>
                    <artifactId>${dependency["artifactId"]}</artifactId>
                    <version>${dependency["version"]}</version>
                    $typeAttribute
                    </dependency>
                    """.trimIndent()

                writer.appendLine(text)
            }

            if (dependencies.isNotEmpty()) {
                writer.appendLine("</dependencies>")
            }

            writer.appendLine("</project>")
        }
        return this
    }

    private fun updateRootMavenMetaData(rootMavenMetaData: TestFile) {
//        val allVersions = rootMavenMetaData.exists() ? new XmlParser().parseText(rootMavenMetaData.text).versioning.versions.version*.value().flatten() : []
//        allVersions << version;
//        publish(rootMavenMetaData) { writer ->
//            val builder = MarkupBuilder(writer)
//            builder.metadata {
//                groupId(groupId)
//                artifactId(artifactId)
//                version(allVersions.max())
//                versioning {
//                    if (uniqueSnapshots && version.endsWith("-SNAPSHOT")) {
//                        snapshot {
//                            timestamp(timestampFormat.format(publishTimestamp))
//                            buildNumber(publishCount)
//                            lastUpdated(updateFormat.format(publishTimestamp))
//                        }
//                    } else {
//                        versions {
//                            allVersions.each {currVersion ->
//                                version(currVersion)
//                            }
//                        }
//                    }
//                }
//            }
//        }
    }

    abstract fun getMetaDataFileContent(): String

    override fun publish(): MavenModule {
        publishPom()
        for (artifact in artifacts) {
            publishArtifact(artifact)
        }
        publishArtifact(mutableMapOf())
        return this
    }

    open fun publishArtifact(artifact: Map<String, String>): File {
        val artifactFile = artifactFile(artifact)
        if (type == "pom") {
            return artifactFile
        }
        publish(artifactFile) { writer -> writer.appendLine("${artifactFile.name} : ${getArtifactContent()}") }
        return artifactFile
    }

    protected fun getArtifactContent(): String {
        // Some content to include in each artifact, so that its size and content varies on each publish
        return (0..publishCount).joinToString("-")
    }

    protected abstract fun publishesMetaDataFile(): Boolean
    protected abstract fun publishesHashFiles(): Boolean

    companion object {
        const val MAVEN_METADATA_FILE = "maven-metadata.xml"

        val updateFormat    = SimpleDateFormat("yyyyMMddHHmmss")
        val timestampFormat = SimpleDateFormat("yyyyMMdd.HHmmss")
    }
}
