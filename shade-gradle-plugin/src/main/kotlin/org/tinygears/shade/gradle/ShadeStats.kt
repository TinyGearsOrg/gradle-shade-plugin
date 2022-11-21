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

import org.gradle.api.GradleException

class ShadeStats {
    var totalTime:     Long = 0
        private set

    var jarStartTime:  Long = 0
        private set

    var jarEndTime:    Long = 0
        private set

    var jarCount:      Int  = 1
        private set

    var processingJar: Boolean = false
        private set

    val relocations:   MutableMap<String, String> = mutableMapOf()

    fun relocate(src: String, dst: String) {
        relocations[src] = dst
    }

    val relocationString: String
        get() {
            val maxLength = relocations.keys.maxOf { it.length }
            return relocations.map { (k, v) -> "$k ${separator(k, maxLength)} $v"}.sorted().joinToString(separator = "\n")
        }

    private fun separator(key: String, max: Int): String {
        return "→"
    }

    fun startJar() {
        if (processingJar) {
            throw GradleException("can only time one entry at a time")
        }
        processingJar = true
        jarStartTime = System.currentTimeMillis()
    }

    fun finishJar() {
        if (processingJar) {
            jarEndTime = System.currentTimeMillis()
            jarCount++
            totalTime += jarTiming
            processingJar = false
        }
    }

    val jarTiming: Long
        get() = jarEndTime - jarStartTime

    val totalTimeSecs: Double
        get() = totalTime / 1000.0

    val averageTimePerJar: Double
        get() = totalTime / jarCount.toDouble()

    val averageTimeSecsPerJar: Double
        get() = averageTimePerJar / 1000.0

    fun printStats() {
        println(this)
    }

    override fun toString(): String {
        return buildString {
            appendLine("*******************")
            appendLine("SHADE STATS")
            appendLine()
            appendLine("Total Jars: $jarCount (includes project)")
            appendLine("Total Time: ${totalTimeSecs}s [${totalTime}ms]")
            appendLine("Average Time/Jar: ${averageTimeSecsPerJar}s [${averageTimePerJar}ms]")
            append("*******************")
        }
    }
}
