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
package org.tinygears.shade.gradle.transformation

import org.tinygears.shade.gradle.ShadeStats
import org.tinygears.shade.gradle.relocation.Relocator
import org.tinygears.shade.gradle.tasks.ShadeCopyAction
import java.io.InputStream

class TransformerContext constructor(val path:       String,
                                     val `is`:       InputStream,
                                     val relocators: List<Relocator>,
                                     val stats:      ShadeStats) {

    companion object {
        fun getEntryTimestamp(preserveFileTimestamps: Boolean, entryTime: Long): Long {
            return if (preserveFileTimestamps) {
                entryTime
            } else {
                ShadeCopyAction.CONSTANT_TIME_FOR_ZIP_ENTRIES
            }
        }
    }
}
