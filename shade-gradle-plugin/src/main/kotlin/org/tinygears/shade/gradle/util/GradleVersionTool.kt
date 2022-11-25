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

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.util.GradleVersion

@Suppress("DEPRECATION")
object GradleVersionTool {

    val version: GradleVersion
        get() = GradleVersion.current()

    fun getSourceSetContainer(project: Project): SourceSetContainer {
        return if (version < GradleVersion.version("7.1")) {
            val convention = project.convention.getPlugin(JavaPluginConvention::class.java)
            convention.sourceSets
        } else {
            val extension = project.extensions.getByType(JavaPluginExtension::class.java)
            extension.sourceSets
        }
    }
}