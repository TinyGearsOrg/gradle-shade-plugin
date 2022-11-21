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
package org.tinygears.shade.gradle.shrinking

import org.gradle.api.file.FileCollection
import org.vafer.jdependency.Clazzpath
import org.vafer.jdependency.ClazzpathUnit
import java.io.File

/**
 * An implementation of an {@code UnusedTracker} that uses jdependency to analyse whether
 * a class is actually being used. This is achieved by loading each individual class and
 * collect its references to other classes. Any class that is not referenced at all from
 * a project unit, i.e. a class that should be kept, can be safely removed.
 * <p>
 * This approach is effective, however it fails to shrink all unused classes as it operates
 * only on class level rather than on each used method.
 */
class UnusedTrackerUsingJDependency
    private constructor(classDirs:  Iterable<File>,
                        classJars:  FileCollection,
                        toMinimize: FileCollection): UnusedTracker(toMinimize) {

    private val projectUnits: MutableList<ClazzpathUnit>
    private val cp:           Clazzpath = Clazzpath()

    init {
        projectUnits = classDirs.map { cp.addClazzpathUnit(it) }.toMutableList()
        projectUnits.addAll(classJars.map { cp.addClazzpathUnit(it) })
    }

    override fun findUnused(): Set<String> {
        val unused = cp.clazzes.toMutableSet()
        for (cpu in projectUnits) {
            unused.removeAll(cpu.clazzes)
            unused.removeAll(cpu.transitiveDependencies)
        }
        return unused.map { it.name }.toSet()
    }

    override fun addDependency(jarOrDir: File) {
        if (toMinimize.contains(jarOrDir)) {
            cp.addClazzpathUnit(jarOrDir)
        }
    }

    companion object {
        fun forProject(apiJars: FileCollection, sourceSetsClassesDirs: Iterable<File>, toMinimize: FileCollection): UnusedTrackerUsingJDependency {
            return UnusedTrackerUsingJDependency(sourceSetsClassesDirs, apiJars, toMinimize)
        }
    }
}
