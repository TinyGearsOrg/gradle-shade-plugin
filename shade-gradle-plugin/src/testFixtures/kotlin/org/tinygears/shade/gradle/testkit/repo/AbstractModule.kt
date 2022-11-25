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
package org.tinygears.shade.gradle.testkit.repo

import org.tinygears.shade.gradle.testkit.file.TestFile
import java.io.InputStream
import java.io.OutputStream
import java.io.Writer
import java.math.BigInteger
import java.security.MessageDigest
import java.util.*

abstract class AbstractModule {
    /**
     * @param cl A closure that is passed a writer to use to generate the content.
     */
    protected fun publish(file: TestFile, cl: (Writer) -> Unit) {
        val hashBefore = if (file.exists()) getHash(file, "sha1") else null
        val tmpFile = file.parentFile!!.file("${file.name}.tmp")

        tmpFile.writer(Charsets.UTF_8).use {
            cl.invoke(it)
        }

        val hashAfter = getHash(tmpFile, "sha1")
        if (hashAfter == hashBefore) {
            // Already published
            return
        }

        assert(!file.exists() || file.delete())
        assert(tmpFile.renameTo(file))
        onPublish(file)
    }

    protected fun publishWithStream(file: TestFile, cl: (OutputStream) -> Unit) {
        val hashBefore = if (file.exists()) getHash(file, "sha1") else null
        val tmpFile = file.parentFile!!.file("${file.name}.tmp")

        tmpFile.outputStream().use {
            cl.invoke(it)
        }

        val hashAfter = getHash(tmpFile, "sha1")
        if (hashAfter == hashBefore) {
            // Already published
            return
        }

        assert(!file.exists() || file.delete())
        assert(tmpFile.renameTo(file))
        onPublish(file)
    }

    protected abstract fun onPublish(file: TestFile)

    companion object {
        fun getSha1File(file: TestFile): TestFile {
            return getHashFile(file, "sha1")
        }

        fun sha1File(file: TestFile): TestFile {
            return hashFile(file, "sha1", 40)
        }

        fun getMd5File(file: TestFile): TestFile {
            return getHashFile(file, "md5")
        }

        fun md5File(file: TestFile): TestFile {
            return hashFile(file, "md5", 32)
        }

        private fun hashFile(file: TestFile, algorithm: String, len: Int): TestFile {
            val hashFile = getHashFile(file, algorithm)
            val hash = getHash(file, algorithm)
            hashFile.bufferedWriter().use {
                it.write("%0${len}x".format(hash))
            }
            return hashFile
        }

        private fun getHashFile(file: TestFile, algorithm: String): TestFile {
            return file.parentFile!!.file("${file.name}.${algorithm}")
        }

        protected fun getHash(file: TestFile, algorithm: String): BigInteger {
            file.inputStream().use {
                return createHash(it, algorithm.uppercase(Locale.getDefault()))
            }
        }

        fun createHash(`is`: InputStream, algorithm: String): BigInteger {
            val messageDigest = MessageDigest.getInstance(algorithm)
            val buffer = ByteArray(4096)
            while (true) {
                val nread = `is`.read(buffer)
                if (nread < 0) {
                    break
                }
                messageDigest.update(buffer, 0, nread)
            }
            return BigInteger(1, messageDigest.digest())
        }
    }
}
