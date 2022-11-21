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
package org.tinygears.shade.gradle.relocation

import org.tinygears.bat.classfile.editor.Renamer
import org.tinygears.bat.util.JvmClassName
import org.tinygears.bat.util.asInternalClassName
import org.tinygears.shade.gradle.ShadeStats
import org.tinygears.shade.gradle.tasks.ShadeCopyAction.*

class RelocatorRemapper constructor(private val relocators: List<Relocator>,
                                    private val stats:      ShadeStats): Renamer() {

    override fun renameClassName(className: JvmClassName): JvmClassName {
        return map(className.toInternalClassName()).asInternalClassName()
    }

    fun hasRelocators(): Boolean {
        return relocators.isNotEmpty()
    }

    fun map(name: String): String {
        var value = name

        for (relocator in relocators) {
            if (relocator.canRelocatePath(name)) {
                val pathContext = RelocatePathContext(name, stats)
                value = relocator.relocatePath(pathContext)
                break
            }
        }

        return value
    }

    fun mapPath(path: String): String {
        return map(path.substring(0, path.indexOf('.')))
    }

    fun mapPath(path: RelativeArchivePath): String {
        return mapPath(path.pathString)
    }
}
