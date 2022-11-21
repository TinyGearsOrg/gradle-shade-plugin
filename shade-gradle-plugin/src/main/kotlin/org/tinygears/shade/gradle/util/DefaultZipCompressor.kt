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

import org.apache.tools.zip.Zip64Mode
import org.apache.tools.zip.ZipOutputStream
import java.io.File
import java.io.IOException

class DefaultZipCompressor
    constructor(            allowZip64Mode:         Boolean,
                private val entryCompressionMethod: Int): ZipCompressor {

    private val zip64Mode: Zip64Mode = if (allowZip64Mode) Zip64Mode.AsNeeded else Zip64Mode.Never

    override fun createArchiveOutputStream(destination: File): ZipOutputStream {
        try {
            val zipOutputStream = ZipOutputStream(destination)
            zipOutputStream.setUseZip64(zip64Mode)
            zipOutputStream.setMethod(entryCompressionMethod)
            return zipOutputStream
        } catch (e: Exception) {
            val message = "Unable to create ZIP output stream for file %s.".format(destination)
            throw IOException(message, e)
        }
    }
}
