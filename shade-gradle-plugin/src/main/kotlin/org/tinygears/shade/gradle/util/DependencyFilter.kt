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

import groovy.lang.Closure
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec

interface DependencyFilter {
    /**
     * Resolve a FileCollection against the include/exclude rules in the filter
     */
    fun resolve(configuration: FileCollection): FileCollection

    /**
     * Resolve all FileCollections against the include/exclude rules in the filter and combine the results
     */
    fun resolve(configurations: Collection<FileCollection>): FileCollection

    /**
     * Exclude dependencies that match the provided spec.
     */
    fun exclude(spec: Spec<in ResolvedDependency>): DependencyFilter

    /**
     * Include dependencies that match the provided spec.
     */
    fun include(spec: Spec<in ResolvedDependency>): DependencyFilter

    /**
     * Create a spec that matches the provided project notation on group, name, and version
     */
    fun project(notation: Map<String, Any>): Spec<in ResolvedDependency>

    /**
     * Create a spec that matches the default configuration for the provided project path on group, name, and version
     */
    fun project(notation: String): Spec<in ResolvedDependency>

    /**
     * Create a spec that matches dependencies using the provided notation on group, name, and version
     */
    fun dependency(notation: Any): Spec<in ResolvedDependency>

    /**
     * Create a spec that matches the provided dependency on group, name, and version
     */
    fun dependency(dependency: Dependency): Spec<in ResolvedDependency>

    /**
     * Create a spec that matches the provided closure
     */
    fun dependency(spec: Closure<Any?>): Spec<in ResolvedDependency>
}
