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

import org.tinygears.shade.gradle.ShadeStats

/**
 * Modified from org.apache.maven.plugins.shade.relocation.Relocator.java
 *
 * @author Jason van Zyl
 * @author John Engelman
 */
interface Relocator {
    fun canRelocatePath(path: String): Boolean
    fun relocatePath(context: RelocatePathContext): String
    fun canRelocateClass(className: String): Boolean
    fun relocateClass(context: RelocateClassContext): String
    fun applyToSourceContent(sourceContent: String): String

    companion object {
        @JvmStatic
        val ROLE: String = Relocator::class.java.name
    }
}

class RelocateClassContext constructor(val className: String, val stats: ShadeStats)

class RelocatePathContext constructor(val path: String, val stats: ShadeStats)
