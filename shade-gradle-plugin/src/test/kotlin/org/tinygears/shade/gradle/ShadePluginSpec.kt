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

import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.tinygears.shade.gradle.tasks.ShadeJar
import java.io.File
import kotlin.test.*

class ShadePluginSpec: PluginSpecification() {

    @Test
    fun `apply plugin`() {
        val projectName = "myshade"
        val version = "1.0.0"

        val project = ProjectBuilder.builder().withName(projectName).build()
        project.version = version

        project.plugins.apply(ShadePlugin::class.java)
        assertTrue(project.plugins.hasPlugin(ShadePlugin::class.java))

        assertNull(project.tasks.findByName("shadeJar"))

        project.plugins.apply(JavaPlugin::class.java)

        val shadeJar = project.tasks.findByName("shadeJar") as ShadeJar

        assertNotNull(shadeJar)
        assertEquals(shadeJar.archiveBaseName.get(), projectName)
        assertEquals(shadeJar.destinationDirectory.asFile.get(), File(project.buildDir, "libs"))
        assertEquals(shadeJar.archiveVersion.get(), version)
        assertEquals(shadeJar.archiveClassifier.get(), "all")
        assertEquals(shadeJar.archiveExtension.get(), "jar")

        val providedConfig = project.configurations.findByName("provided")
        assertTrue(providedConfig?.artifacts?.files?.contains(shadeJar.archiveFile.get().asFile) ?: false)
    }

    @ParameterizedTest
    @ValueSource(strings = [ "7.0", "7.1", "7.2" ])
    fun `compatible with gradle #version`(version: String) {
        val versionRunner =
            runner.withGradleVersion(version)
                  .withArguments("--stacktrace")
                  .withDebug(true)

        val one =
            buildJar("one.jar")
                .insertFile("META-INF/services/shadow.Shadow", "one # NOTE: No newline terminates this line/file")
                .write()

        repo.module("shade", "two", "1.0")
            .insertFile("META-INF/services/shadow.Shadow", "two # NOTE: No newline terminates this line/file")
            .publish()

        buildFile.appendText(
            """
            dependencies {
              implementation 'junit:junit:3.8.2'
              implementation files('${one.escapedPath()}')
            }

            shadeJar {}
            """.trimIndent()
        )

        versionRunner.withArguments("shadeJar", "--stacktrace").build()

        assertTrue(output.exists())
    }

    @Test
    fun `error in gradle version less than 7`() {
        val versionRunner =
            runner.withGradleVersion("6.9")
                  .withArguments("--stacktrace")
                  .withDebug(true)
                  .withTestKitDir(testKitDir)

        buildFile.appendText(
            """
            dependencies {
              implementation 'junit:junit:3.8.2'
            }

            shadowJar {}
            """.trimIndent()
        )

        versionRunner.withArguments("shadeJar", "--stacktrace").buildAndFail()
        assertFalse(output.exists())
    }
}
