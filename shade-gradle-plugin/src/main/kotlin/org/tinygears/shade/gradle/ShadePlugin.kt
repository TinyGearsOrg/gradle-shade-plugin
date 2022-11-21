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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class ShadePlugin: Plugin<Project> {
    @Override
    override fun apply(project: Project) {
        project.plugins.apply(ShadeBasePlugin::class.java)
        project.plugins.withType(JavaPlugin::class.java) {
            project.plugins.apply(ShadeJavaPlugin::class.java)
        }

//        plugins.withType(ApplicationPlugin) {
//            plugins.apply(ShadowApplicationPlugin)
//        }
    }

    companion object {
        internal const val SHADE_EXTENSION_NAME        = "shade"
        internal const val SHADE_TASK_NAME             = "shadeJar"
        internal const val PROVIDED_CONFIGURATION_NAME = "provided"
        internal const val SHADE_GROUP                 = "Shade"
        internal const val SHADE_RUNTIME_ELEMENT       = "shadeRuntimeElement"
    }
}
