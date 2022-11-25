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
package org.tinygears.shade.gradle.testkit.file

import groovy.lang.Closure
import org.apache.commons.io.FileUtils
import org.apache.tools.ant.Project
import org.apache.tools.ant.Task
import org.apache.tools.ant.taskdefs.Tar
import org.apache.tools.ant.taskdefs.Zip
import org.apache.tools.ant.types.EnumeratedAttribute
import org.codehaus.groovy.runtime.ResourceGroovyMethods
import org.junit.jupiter.api.Assertions.*
import java.io.*
import java.net.URI
import java.net.URL
import java.nio.charset.Charset
import java.security.MessageDigest
import java.util.*
import java.util.jar.JarFile
import java.util.jar.Manifest

class TestFile(file: File, vararg path: Any): File(join(file, path).absolutePath) {
    private var useNativeTools = false

    constructor(uri: URI): this(File(uri))
    constructor(path: String): this(File(path))
    constructor(url: URL): this(toUri(url))

    fun usingNativeTools(): TestFile {
        useNativeTools = true
        return this
    }

    fun writeReplace(): Any {
        return File(absolutePath)
    }

    override fun getCanonicalFile(): File {
        return File(absolutePath).canonicalFile
    }

    override fun getCanonicalPath(): String {
        return File(absolutePath).canonicalPath
    }

    fun file(vararg path: Any): TestFile {
        return try {
            TestFile(this, *path)
        } catch (e: RuntimeException) {
            throw RuntimeException("could not locate file '%s' relative to '%s'.".format(path.contentToString(), this), e)
        }
    }

    fun files(vararg paths: Any): List<TestFile> {
        val files: MutableList<TestFile> = ArrayList()
        for (path in paths) {
            files.add(file(path))
        }
        return files
    }

    fun withExtension(extension: String): TestFile {
        return parentFile!!.file(name.replace("\\..*$".toRegex(), ".$extension"))
    }

    fun writelns(vararg lines: String): TestFile {
        return writelns(listOf(*lines))
    }

    fun write(content: Any): TestFile {
        try {
            outputStream().use { it.write(content.toString().encodeToByteArray()) }
        } catch (e: IOException) {
            throw RuntimeException(String.format("could not write to test file '%s'", this), e)
        }
        return this
    }

    fun leftShift(content: Any): TestFile {
        parentFile!!.mkdirs()
        return try {
            ResourceGroovyMethods.leftShift(this, content)
            this
        } catch (e: IOException) {
            throw RuntimeException(String.format("could not append to test file '%s'", this), e)
        }
    }

    override fun listFiles(): Array<TestFile> {
        val children = super.listFiles()
        val files = mutableListOf<TestFile>()
        for (i in children!!.indices) {
            val child = children[i]
            files.add(TestFile(child))
        }
        return files.toTypedArray()
    }

    val text: String
        get() {
            assertIsFile()
            return try {
                this.readText(Charset.defaultCharset())
            } catch (e: IOException) {
                throw RuntimeException(String.format("could not read from test file '%s'", this), e)
            }
        }

    val properties: Map<String, String>
        get() {
            assertIsFile()
            val properties = Properties()
            FileInputStream(this).use { inStream -> properties.load(inStream) }
            val map = mutableMapOf<String, String>()
            for (key in properties.keys) {
                map[key.toString()] = properties.getProperty(key.toString())
            }
            return map
        }

    val manifest: Manifest
        get() {
            assertIsFile()
            return JarFile(this).use { jarFile -> jarFile.manifest }
        }

//    fun linesThat(matcher: Matcher<in String>): List<String> {
//        return BufferedReader(FileReader(this)).use { reader ->
//            val lines: MutableList<String> = ArrayList()
//            var line: String
//            while (reader.readLine().also { line = it } != null) {
//                if (matcher.matches(line)) {
//                    lines.add(line)
//                }
//            }
//            lines
//        }
//    }

    fun unzipTo(target: File) {
        assertIsFile()
        TestFileHelper(this).unzipTo(target, useNativeTools)
    }

    fun untarTo(target: File) {
        assertIsFile()
        TestFileHelper(this).untarTo(target, useNativeTools)
    }

    fun copyTo(target: File) {
        if (isDirectory) {
            try {
                FileUtils.copyDirectory(this, target)
            } catch (e: IOException) {
                throw RuntimeException("could not copy test directory '%s' to '%s'".format(this, target), e)
            }
        } else {
            try {
                FileUtils.copyFile(this, target)
            } catch (e: IOException) {
                throw RuntimeException("could not copy test file '%s' to '%s'".format(this, target), e)
            }
        }
    }

    fun copyFrom(target: File) {
        TestFile(target).copyTo(this)
    }

    fun copyFrom(resource: URL) {
        try {
            FileUtils.copyURLToFile(resource, this)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun moveToDirectory(target: File) {
        if (target.exists() && !target.isDirectory) {
            throw RuntimeException("target '$target' is not a directory")
        }
        try {
            FileUtils.moveFileToDirectory(this, target, true)
        } catch (e: IOException) {
            throw RuntimeException("could not move test file '$this' to directory '$target'", e)
        }
    }

    fun touch(): TestFile {
        FileUtils.touch(this)
        assertIsFile()
        return this
    }

    /**
     * Creates a directory structure specified by the given closure.
     * <pre>
     * dir.create {
     * subdir1 {
     * file 'somefile.txt'
     * }
     * subdir2 { nested { file 'someFile' } }
     * }
    </pre> *
     */
    fun create(structure: Closure<*>): TestFile {
        assertTrue(isDirectory || mkdirs())
        TestWorkspaceBuilder(this).apply(structure)
        return this
    }

    override fun getParentFile(): TestFile? {
        return if (super.getParentFile() == null) null else TestFile(super.getParentFile())
    }

    override fun toString(): String {
        return path
    }

    fun writelns(lines: Iterable<String>): TestFile {
        val formatter = Formatter()
        for (line in lines) {
            formatter.format("%s%n", line)
        }
        return write(formatter)
    }

    fun assertExists(): TestFile {
        assertTrue(exists()) {"$this does not exist" }
        return this
    }

    fun assertIsFile(): TestFile {
        assertTrue(isFile) { "$this is not a file" }
        return this
    }

    fun assertIsDir(): TestFile {
        assertTrue(isDirectory) { "$this is not a directory" }
        return this
    }

    fun assertDoesNotExist(): TestFile {
        assertFalse(exists()) { "$this should not exist" }
        return this
    }

//    fun assertContents(matcher: Matcher<String>?): TestFile {
//        MatcherAssert.assertThat(text, matcher)
//        return this
//    }

    fun assertIsCopyOf(other: TestFile): TestFile {
        assertIsFile()
        other.assertIsFile()
        assertEquals(other.length(), length()) { "$this is not the same length as $other" }
        assertTrue(getHash("MD5").contentEquals(other.getHash("MD5"))) { "$this does not have the same content as $other" }
        return this
    }

    private fun getHash(algorithm: String): ByteArray {
        return try {
            val messageDigest: MessageDigest = MessageDigest.getInstance(algorithm)
            messageDigest.update(readBytes())
            messageDigest.digest()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun readLink(): String {
        assertExists()
        return TestFileHelper(this).readLink()
    }

    val permissions: String
        get() {
            assertExists()
            return TestFileHelper(this).permissions
        }

    fun setPermissions(permissions: String): TestFile {
        assertExists()
        TestFileHelper(this).permissions = permissions
        return this
    }

    var mode: Int
        get() {
            assertExists()
            return TestFileHelper(this).mode
        }
        set(mode) {
            assertExists()
            TestFileHelper(this).mode = mode
        }

    /**
     * Asserts that this file contains exactly the given set of descendants.
     */
    fun assertHasDescendants(vararg descendants: String): TestFile {
        val actual: MutableSet<String> = TreeSet()
        assertIsDir()
        visit(actual, "", this)
        val expected: Set<String> = TreeSet(listOf(*descendants))
        val extras: MutableSet<String> = TreeSet(actual)
        extras.removeAll(expected)
        val missing: MutableSet<String> = TreeSet(expected)
        missing.removeAll(actual)
        assertEquals(expected, actual) { "for dir: %s, extra files: %s, missing files: %s, expected: %s".format(this, extras, missing, expected) }
        return this
    }

    fun assertIsEmptyDir(): TestFile {
        if (exists()) {
            assertIsDir()
            assertHasDescendants()
        }
        return this
    }

    private fun visit(names: MutableSet<String>, prefix: String, file: File) {
        for (child in file.listFiles()!!) {
            if (child.isFile) {
                names.add(prefix + child.name)
            } else if (child.isDirectory) {
                visit(names, prefix + child.name + "/", child)
            }
        }
    }

    fun isSelfOrDescendent(file: File): Boolean {
        return if (file.absolutePath == absolutePath) {
            true
        } else {
            file.absolutePath.startsWith(absolutePath + separatorChar)
        }
    }

    fun createDir(): TestFile {
        if (mkdirs()) {
            return this
        }
        if (isDirectory) {
            return this
        }
        throw AssertionError("Problems creating dir: $this. Diagnostics: exists=${exists()}, isFile=${isFile}, isDirectory=${isDirectory}")
    }

    fun createDir(path: Any): TestFile {
        return TestFile(this, path).createDir()
    }

    fun deleteDir(): TestFile {
        TestFileHelper(this).delete(useNativeTools)
        return this
    }

    /**
     * Attempts to delete this directory, ignoring failures to do so.
     * @return this
     */
    fun maybeDeleteDir(): TestFile {
        try {
            deleteDir()
        } catch (e: RuntimeException) {
            // Ignore
        }
        return this
    }

    fun createFile(): TestFile {
        TestFile(parentFile!!).createDir()
        try {
            assertTrue(isFile || createNewFile())
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
        return this
    }

    fun createFile(path: Any): TestFile {
        return file(path).createFile()
    }

    fun createZip(path: Any): TestFile {
        val zip = Zip()
        zip.setWhenempty(Zip.WhenEmpty.getInstance(Zip.WhenEmpty::class.java, "create") as Zip.WhenEmpty)
        val zipFile = file(path)
        zip.destFile = zipFile
        zip.setBasedir(this)
        zip.setExcludes("**")
        execute(zip)
        return zipFile
    }

    fun zipTo(zipFile: TestFile): TestFile {
        TestFileHelper(this).zipTo(zipFile, useNativeTools)
        return this
    }

    fun tarTo(tarFile: TestFile): TestFile {
        TestFileHelper(this).tarTo(tarFile, useNativeTools)
        return this
    }

    fun tgzTo(tarFile: TestFile): TestFile {
        val tar = Tar()
        tar.setBasedir(this)
        tar.setDestFile(tarFile)
        tar.setCompression(EnumeratedAttribute.getInstance(Tar.TarCompressionMethod::class.java, "gzip") as Tar.TarCompressionMethod)
        execute(tar)
        return this
    }

    fun tbzTo(tarFile: TestFile): TestFile {
        val tar = Tar()
        tar.setBasedir(this)
        tar.setDestFile(tarFile)
        tar.setCompression(EnumeratedAttribute.getInstance(Tar.TarCompressionMethod::class.java, "bzip2") as Tar.TarCompressionMethod)
        execute(tar)
        return this
    }

    private fun execute(task: Task) {
        task.project = Project()
        task.execute()
    }

    fun snapshot(): Snapshot {
        assertIsFile()
        return Snapshot(lastModified(), getHash("MD5"))
    }

    fun assertHasChangedSince(snapshot: Snapshot) {
        val now = snapshot()
        assertTrue(now.modTime != snapshot.modTime || !now.hash.contentEquals(snapshot.hash))
    }

    fun assertContentsHaveChangedSince(snapshot: Snapshot) {
        val now = snapshot()
        assertTrue(!now.hash.contentEquals(snapshot.hash)) { "contents of $this have not changed" }
    }

    fun assertContentsHaveNotChangedSince(snapshot: Snapshot) {
        val now = snapshot()
        assertArrayEquals(snapshot.hash, now.hash) { "contents of $this has changed" }
    }

    fun assertHasNotChangedSince(snapshot: Snapshot) {
        val now = snapshot()
        assertEquals(snapshot.modTime, now.modTime) { "last modified time of $this has changed" }
        assertArrayEquals(snapshot.hash, now.hash) { "contents of $this has changed" }
    }

    fun writeProperties(properties: Map<*, *>) {
        val props = Properties()
        props.putAll(properties)
        FileOutputStream(this).use { stream -> props.store(stream, "comment") }
    }

    fun exec(vararg args: String): ExecOutput {
        return TestFileHelper(this).execute(listOf(*args), null)
    }

    fun execute(args: List<String>, env: List<String>): ExecOutput {
        return TestFileHelper(this).execute(args, env)
    }

    inner class Snapshot(val modTime: Long, val hash: ByteArray)

    companion object {
        private fun toUri(url: URL): URI {
            return url.toURI()
        }

        private fun join(file: File, path: Array<out Any>): File {
            var current = file.absoluteFile
            for (p in path) {
                current = File(current, p.toString())
            }
            return try {
                current.canonicalFile
            } catch (e: IOException) {
                throw RuntimeException(String.format("Could not canonicalize '%s'.", current), e)
            }
        }
    }
}
