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
package org.tinygears.shade.gradle.testkit.file;

import org.apache.commons.io.FileUtils
import org.apache.commons.lang3.StringUtils
import org.apache.tools.ant.Project
import org.apache.tools.ant.taskdefs.Expand
import org.apache.tools.ant.taskdefs.Tar
import org.apache.tools.ant.taskdefs.Untar
import org.apache.tools.ant.taskdefs.Zip
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.io.OutputStream

import java.util.zip.ZipInputStream

class TestFileHelper constructor(private val file: TestFile) {
    fun unzipTo(target: File, nativeTools: Boolean) {
        // Check that each directory in hierarchy is present
        file.inputStream().use { instr ->
            val dirs = mutableSetOf<String>()
            ZipInputStream(instr).use { zipInputStream ->
                generateSequence { zipInputStream.nextEntry }.map { entry ->
                    if (entry.isDirectory) {
                        assertTrue(dirs.add(entry.name)) { "duplicate directory '$entry.name'" }
                    }
                    if (entry.name.contains('/')) {
                        val parent = StringUtils.substringBeforeLast(entry.name, "/") + '/'
                        assertTrue(dirs.contains(parent)) { "missing dir '$parent'" }
                    }
                }.toList()
            }
        }

        if (nativeTools && isUnix()) {
            val process = ProcessBuilder(listOf("unzip", "-o", file.absolutePath, "-d", target.absolutePath)).execute()
            process.consumeProcessOutput(System.out, System.err)
            assert(process.waitFor() == 0)
            return
        }

        val unzip = Expand()
        unzip.setSrc(file)
        unzip.setDest(target)

        unzip.project = Project()
        unzip.execute()
    }

    fun untarTo(target: File, nativeTools: Boolean) {
        if (nativeTools && isUnix()) {
            target.mkdirs()
            val builder = ProcessBuilder(listOf("tar", "-xpf", file.absolutePath))
            builder.directory(target)
            val process = builder.start()
            process.consumeProcessOutput()
            assert(process.waitFor() == 0)
            return
        }

        val untar = Untar()
        untar.setSrc(file)
        untar.setDest(target)

        if (file.name.endsWith(".tgz")) {
            val method = Untar.UntarCompressionMethod()
            method.value = "gzip"
            untar.setCompression(method)
        } else if (file.name.endsWith(".tbz2")) {
            val method = Untar.UntarCompressionMethod()
            method.value = "bzip2"
            untar.setCompression(method)
        }

        untar.project = Project()
        untar.execute()
    }

    private fun isUnix(): Boolean {
        return !System.getProperty("os.name").toLowerCase().contains("windows")
    }

    var permissions: String
        get() {
            if (!isUnix()) {
                return "-rwxr-xr-x"
            }

            val process = ProcessBuilder(listOf("ls", "-ld", file.absolutePath)).execute()
            val result = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            val retval = process.waitFor()
            if (retval != 0) {
                throw RuntimeException("could not list permissions for '$file': $error")
            }
            val perms = result.split(' ')[0]
            assert(perms.matches("[d\\-][rwx\\-]{9}[@\\+]?".toRegex()))
            return perms.substring(1, 10)
        }
        set(value) {
            if (!isUnix()) {
                return
            }
            val m = toMode(value)
            mode = m
        }

    var mode: Int
        get() = toMode(permissions)
        set(value) {
            val process = ProcessBuilder(listOf("chmod", Integer.toOctalString(value), file.absolutePath)).execute()
            val error = process.errorStream.bufferedReader().readText()
            val retval = process.waitFor()
            if (retval != 0) {
                throw RuntimeException("could not set permissions for '$file': $error")
            }
        }

    private fun toMode(permissions: String): Int {
        val m = listOf(6, 3, 0).fold(0) { mode, pos ->
            var r = mode
            r = r or if (permissions[9 - pos - 3] == 'r') 4 shl pos else 0
            r = r or if (permissions[9 - pos - 2] == 'w') 2 shl pos else 0
            r = r or if (permissions[9 - pos - 1] == 'x') 1 shl pos else 0
            return r
        }
        return m
    }

    fun delete(nativeTools: Boolean) {
        if (isUnix() && nativeTools) {
            val process = ProcessBuilder(listOf("rm", "-rf", file.absolutePath)).execute()
            val error = process.errorStream.bufferedReader().readText()
            val retval = process.waitFor()
            if (retval != 0) {
                throw RuntimeException("could not delete '$file': $error")
            }
        } else {
            FileUtils.deleteQuietly(file);
        }
    }

    fun readLink(): String {
        val process = ProcessBuilder(listOf("readlink", file.absolutePath)).execute()
        val error = process.errorStream.bufferedReader().readText()
        val retval = process.waitFor()
        if (retval != 0) {
            throw RuntimeException("could not read link '$file': $error")
        }
        return process.inputStream.bufferedReader().readText().trim()
    }

    fun exec(args: List<String>): ExecOutput {
        return execute(args, null)
    }

    fun execute(args: List<String>, env: List<String>?): ExecOutput {
        val process = ProcessBuilder(listOf(file.absolutePath).plus(args)).execute(env, null)
        val output = process.inputStream.bufferedReader().readText()
        val error  = process.errorStream.bufferedReader().readText()
        if (process.waitFor() != 0) {
            throw RuntimeException("Could not execute $file. Error: $error, Output: $output")
        }
        return ExecOutput(output, error)
    }

    fun zipTo(zipFile: TestFile, nativeTools: Boolean) {
        if (nativeTools && isUnix()) {
            val process = ProcessBuilder(listOf("zip", zipFile.absolutePath, "-r", file.name)).execute(dir = zipFile.parentFile)
            process.consumeProcessOutput(System.out, System.err)
            assert(process.waitFor() == 0)
        } else {
            val zip = Zip()
            zip.setBasedir(file)
            zip.destFile = zipFile
            zip.project = Project()
            val whenEmpty = Zip.WhenEmpty()
            whenEmpty.value = "create"
            zip.setWhenempty(whenEmpty)
            zip.execute()
        }
    }

    fun tarTo(tarFile: TestFile, nativeTools: Boolean) {
        if (nativeTools && isUnix()) {
            val process = ProcessBuilder(listOf("tar", "-cf", tarFile.absolutePath, file.name)).execute(dir = tarFile.parentFile)
            process.consumeProcessOutput(System.out, System.err)
            assert(process.waitFor() == 0)
        } else {
            val tar = Tar()
            tar.setBasedir(file)
            tar.setDestFile(tarFile)
            tar.project = Project()
            tar.execute()
        }
    }
}

fun ProcessBuilder.execute(env: List<String>? = null, dir: File? = null): Process {
    if (env != null) {
        val e = environment()

        for (envPair in env) {
            val (key, value) = envPair.split("=")
            e[key] = value
        }
    }

    if (dir != null) {
        directory(dir)
    }

    return start()
}

fun Process.consumeProcessOutput() {
    this.inputStream.readBytes()
    this.errorStream.readBytes()
}

fun Process.consumeProcessOutput(output: OutputStream, error: OutputStream) {
    this.inputStream.copyTo(output)
    this.errorStream.copyTo(error)
}