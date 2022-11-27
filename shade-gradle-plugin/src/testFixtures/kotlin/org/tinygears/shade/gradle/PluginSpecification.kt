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

import org.gradle.internal.impldep.com.google.common.base.StandardSystemProperty
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.io.TempDir
import org.tinygears.shade.gradle.testkit.AppendableMavenFileRepository
import org.tinygears.shade.gradle.testkit.AppendableJar
import org.tinygears.shade.gradle.testkit.file.TestFile
import java.io.File
import java.nio.file.Path

import java.util.jar.JarEntry
import java.util.jar.JarFile

abstract class PluginSpecification {

    @TempDir
    lateinit var dir: Path

    lateinit var repo: AppendableMavenFileRepository

    val localRepo: File
        get() {
            val rootRelative = File("build/localrepo")
            return if (rootRelative.isDirectory) rootRelative else File(File(StandardSystemProperty.USER_DIR.value()).parentFile, "build/localrepo")
        }

    val buildFile: File
        get() = file("build.gradle")

    val settingsFile: File
        get() = file("settings.gradle")

    val output: File
        get() = getFile("build/libs/shade-1.0-all.jar")

    val runner: GradleRunner
        get() = GradleRunner.create()
                            .withProjectDir(dir.toFile())
                            .forwardOutput()
                            .withPluginClasspath()

    @BeforeEach
    open fun setup() {
        repo = repo()
        repo.module("junit", "junit", "3.8.2").use(getTestJar()).publish()

        buildFile.appendText(getDefaultBuildScript())
        settingsFile.appendText(
            """
            rootProject.name = 'shade'
            
            """.trimIndent()
        )
    }

    @AfterEach
    fun cleanup() {
        println(buildFile.bufferedReader().readText())
    }

    fun getDefaultBuildScript(javaPlugin: String = "java"): String {
        return """
        plugins {
            id '${javaPlugin}'
            id 'org.tinygears.shade' version "${Version.version}"
        }

        version = "1.0"
        group = 'shadow'

        sourceSets {
          integTest
        }

        repositories { maven { url "${repo.getUri()}" } }
        
        """.trimIndent()
    }

    fun run(vararg tasks: String): BuildResult {
        return run(tasks.toList())
    }

    fun runWithDeprecationWarnings(vararg tasks: String): BuildResult {
        return runner(tasks.toList()).build()
    }

    fun run(tasks: List<String>): BuildResult {
        val result = runner(tasks).build()
        assertNoDeprecationWarnings(result)
        return result
    }

    fun runWithDebug(vararg tasks: String): BuildResult {
        val result = runner(tasks.toList()).withDebug(true).build()
        assertNoDeprecationWarnings(result)
        return result
    }

    fun runner(tasks: Collection<String>): GradleRunner {
        return runner.withArguments(listOf("-Dorg.gradle.warning.mode=all") + tasks.toList())
    }

    private fun assertNoDeprecationWarnings(result: BuildResult) {
        result.output.lines().forEach {
            assert(!containsDeprecationWarning(it))
        }
    }

    fun file(path: String): File {
        val f = File(dir.toFile(), path)
        if (!f.exists()) {
            f.parentFile.mkdirs()
            return dir.resolve(path).toFile()
        }
        return f
    }

    fun getFile(path: String): File {
        return File(dir.toFile(), path)
    }

    fun repo(path: String = "maven-repo"): AppendableMavenFileRepository {
        return AppendableMavenFileRepository(TestFile(dir.toFile(), path))
    }

    fun assertJarFileContentsEqual(f: File, path: String, contents: String) {
        assert(getJarFileContents(f, path) == contents)
    }

    fun getJarFileContents(f: File, path: String): String {
        val jf = JarFile(f)

        jf.use {
            it.getInputStream(JarEntry(path)).use { `is` ->
                return `is`.bufferedReader().readText()
            }
        }
    }

    fun contains(f: File, paths: List<String>) {
        val jar = JarFile(f)
        jar.use {
            for (path in paths) {
                assert(it.getJarEntry(path) != null) { "${f.path} does not contain [$path]" }
            }
        }
    }

    fun doesNotContain(f: File, paths: List<String>) {
        val jar = JarFile(f)
        jar.use {
            for (path in paths) {
                assert(jar.getJarEntry(path) == null) { "${f.path} contains [$path]" }
            }
        }
    }

    fun buildJar(path: String): AppendableJar {
        return AppendableJar(file(path))
    }

    fun output(name: String): File {
        return getFile("build/libs/${name}")
    }

    protected fun getTestJar(name: String = "junit-3.8.2.jar"): File {
        return File(this::class.java.classLoader.getResource(name).toURI())
    }

    companion object {
        val testKitDir: File
            get() {
                var gradleUserHome = System.getenv("GRADLE_USER_HOME")
                if (gradleUserHome == null) {
                    gradleUserHome = File(System.getProperty("user.home"), ".gradle").absolutePath
                }
                return File(gradleUserHome, "testkit")
            }

        private fun containsDeprecationWarning(output: String): Boolean {
            return output.contains("has been deprecated and is scheduled to be removed in Gradle") ||
                   output.contains("has been deprecated. This is scheduled to be removed in Gradle")
        }
    }
}

fun File.escapedPath(): String {
    return this.path.replace("\\\\", "\\\\\\\\")
}