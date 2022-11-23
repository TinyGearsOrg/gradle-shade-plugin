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
package org.tinygears.shade.gradle.tasks

import org.gradle.api.Action
import org.gradle.api.file.CopySpec
import org.tinygears.shade.gradle.ShadeStats
import org.tinygears.shade.gradle.relocation.Relocator
import org.tinygears.shade.gradle.shrinking.R8Configuration
import org.tinygears.shade.gradle.transformation.Transformer
import org.tinygears.shade.gradle.util.DependencyFilter

interface ShadeSpec: CopySpec {
    fun useR8(): ShadeSpec
    fun useR8(configure: Action<R8Configuration>?): ShadeSpec
    fun minimize(): ShadeSpec
    fun minimize(configureClosure: Action<DependencyFilter>?): ShadeSpec
    fun dependencies(configure: Action<DependencyFilter>?): ShadeSpec

    fun transform(clazz: Class<out Transformer>): ShadeSpec
    fun <T: Transformer> transform(clazz: Class<T>, configure: Action<T>?): ShadeSpec
    fun transform(transformer: Transformer): ShadeSpec

    //fun mergeServiceFiles(): ShadeSpec
    //fun mergeServiceFiles(rootPath: String?): ShadeSpec
    //fun mergeServiceFiles(configureClosure: Action<ServiceFileTransformer>?): ShadeSpec
    //fun mergeGroovyExtensionModules(): ShadeSpec
    //fun append(resourcePath: String?): ShadeSpec

    fun relocate(pattern: String, destination: String): ShadeSpec
    fun relocate(pattern: String, destination: String, configure: Action<Relocator>?): ShadeSpec
    fun relocate(relocator: Relocator): ShadeSpec
    fun relocate(clazz: Class<out Relocator>): ShadeSpec
    fun <R: Relocator> relocate(clazz: Class<R>, configure: Action<R>?): ShadeSpec

    val stats: ShadeStats
}