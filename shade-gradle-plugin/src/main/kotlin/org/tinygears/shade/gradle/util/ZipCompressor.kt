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

package org.tinygears.shade.gradle.util

import org.gradle.api.internal.file.archive.compression.ArchiveOutputStreamFactory
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.bundling.ZipEntryCompression
import java.io.File
import java.io.IOException
import kotlin.jvm.Throws

fun getInternalCompressor(entryCompression: ZipEntryCompression, jar: Jar): ZipCompressor {
    return when (entryCompression) {
        ZipEntryCompression.DEFLATED -> DefaultZipCompressor(jar.isZip64, ZipOutputStream.DEFLATED)
        ZipEntryCompression.STORED   -> DefaultZipCompressor(jar.isZip64, ZipOutputStream.STORED)
        else -> throw IllegalArgumentException("Unknown Compression type %s".format(entryCompression))
    }
}

interface ZipCompressor: ArchiveOutputStreamFactory {
    @Throws(IOException::class)
    override fun createArchiveOutputStream(destination: File): ZipOutputStream
}
