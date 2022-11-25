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
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.util.GradleVersion
import org.tinygears.shade.gradle.ShadePlugin.Companion.PROVIDED_CONFIGURATION_NAME
import org.tinygears.shade.gradle.ShadePlugin.Companion.SHADE_EXTENSION_NAME

internal class ShadeBasePlugin: Plugin<Project> {

    @Override
    override fun apply(project: Project) {
        if (GradleVersion.current() < GradleVersion.version("7.0")) {
            throw GradleException("The shade plugin supports gradle 7.0+ only. Please upgrade.")
        }

        // Add the extension object
        project.extensions.create(SHADE_EXTENSION_NAME, ShadeExtension::class.java, project)
        createProvidedConfigurationIfNeeded(project)
    }

    private fun createProvidedConfigurationIfNeeded(project: Project) {
        val configuration = project.configurations.findByName(PROVIDED_CONFIGURATION_NAME)
        if (configuration == null) {
            project.configurations.create(PROVIDED_CONFIGURATION_NAME)
        }
    }
}
