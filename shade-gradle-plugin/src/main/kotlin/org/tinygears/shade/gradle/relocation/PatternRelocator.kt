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

import org.tinygears.bat.util.fileNameMatcher
import org.tinygears.shade.gradle.util.Cacheable
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import java.util.regex.Pattern

/**
 * Modified from org.apache.maven.plugins.shade.relocation.SimpleRelocator.java
 *
 * @author Jason van Zyl
 * @author Mauro Talevi
 * @author John Engelman
 */
@Cacheable
class PatternRelocator
    constructor(pattern:       String,
                shadedPattern: String,
                includes:      List<String>,
                excludes:      List<String>,
                rawString:     Boolean = false): Relocator {

    @Input
    @Optional
    private val pattern: String = pattern.replace('/', '.')

    @Input
    @Optional
    private val shadedPattern: String = shadedPattern.replace('/', '.')

    @Input
    private val pathPattern: String = pattern.replace('.', '/')

    @Input
    private val shadedPathPattern: String = shadedPattern.replace('.', '/')

    @Input
    private val includes: MutableSet<String> = normalizePatterns(includes)

    @Input
    private val excludes: MutableSet<String> = normalizePatterns(excludes)

    @Input
    private val rawString: Boolean = rawString

    fun include(pattern: String): PatternRelocator {
        includes.addAll(normalizePatterns(listOf(pattern)))
        return this
    }

    fun exclude(pattern: String): PatternRelocator {
        excludes.addAll(normalizePatterns(listOf(pattern)))
        return this
    }

    private fun isIncluded(path: String): Boolean {
        if (includes.isNotEmpty()) {
            for (include in includes) {
                if (fileNameMatcher(include).matches(path)) {
                    return true
                }
            }
            return false
        }
        return true
    }

    private fun isExcluded(path: String): Boolean {
        if (excludes.isNotEmpty()) {
            for (exclude in excludes) {
                if (fileNameMatcher(exclude).matches(path)) {
                    return true
                }
            }
        }
        return false
    }

    override fun canRelocatePath(path: String): Boolean {
        if (rawString) {
            return Pattern.compile(pathPattern).matcher(path).find()
        }

        // If string is too short - no need to perform expensive string operations
        if (path.length < pathPattern.length) {
            return false
        }

        var normalizedPath = path
        if (path.endsWith(".class")) {
            normalizedPath = path.removeSuffix(".class")
        }

        if (normalizedPath.isEmpty()) {
            return false
        }

        // Allow for annoying option of an extra / on the front of a path. See MSHADE-119 comes from getClass().getResource("/a/b/c.properties").
        normalizedPath = normalizedPath.removePrefix("/")

        val pathStartsWithPattern = normalizedPath.startsWith(pathPattern)
        if (pathStartsWithPattern) {
            return isIncluded(normalizedPath) && !isExcluded(normalizedPath)
        }

        return false
    }

    override fun canRelocateClass(className: String): Boolean {
        return !rawString                  &&
                className.indexOf('/') < 0 &&
                canRelocatePath(className.replace('.', '/'))
    }

    override fun relocatePath(context: RelocatePathContext): String {
        val path = context.path
        val shadedPath = if (rawString) {
            path.replace(pathPattern, shadedPathPattern)
        } else {
            path.replaceFirst(pathPattern, shadedPathPattern)
        }
        context.stats.relocate(path, shadedPath)
        return shadedPath
    }

    override fun relocateClass(context: RelocateClassContext): String {
        val clazz = context.className
        val shadedClazz = clazz.replaceFirst(pattern, shadedPattern)
        context.stats.relocate(clazz, shadedClazz)
        return shadedClazz
    }

    override fun applyToSourceContent(sourceContent: String): String {
        return if (rawString) {
            sourceContent
        } else {
            sourceContent.replace("\\b$pattern", shadedPattern)
        }
    }

    companion object {
        private const val REGEX_HANDLER_PREFIX = "%regex"

        private fun normalizePatterns(patterns: Collection<String>): MutableSet<String> {
            val normalized = LinkedHashSet<String>()

            for (pattern in patterns) {
                // Regex patterns don't need to be normalized and stay as is
                if (pattern.startsWith(REGEX_HANDLER_PREFIX)) {
                    normalized.add(pattern)
                    continue
                }

                val classPattern = pattern.replace('.', '/')
                normalized.add(classPattern)

                if (classPattern.endsWith("/*")) {
                    val packagePattern = classPattern.substring(0, classPattern.lastIndexOf('/'))
                    normalized.add(packagePattern)
                }
            }

            return normalized
        }
    }
}
