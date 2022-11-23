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
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.specs.Spec
import org.gradle.api.specs.Specs

internal abstract class AbstractDependencyFilter constructor(val project: Project): DependencyFilter {

    protected val includeSpecs: MutableList<Spec<in ResolvedDependency>> = mutableListOf()
    protected val excludeSpecs: MutableList<Spec<in ResolvedDependency>> = mutableListOf()

    protected abstract fun resolve(dependencies:         Set<ResolvedDependency>,
                                   includedDependencies: MutableSet<ResolvedDependency>,
                                   excludedDependencies: MutableSet<ResolvedDependency>)

    override fun resolve(configuration: FileCollection): FileCollection {
        val includedDeps = mutableSetOf<ResolvedDependency>()
        val excludedDeps = mutableSetOf<ResolvedDependency>()

        if (configuration is Configuration) {
            resolve(configuration.resolvedConfiguration.firstLevelModuleDependencies, includedDeps, excludedDeps)
            return project.files(configuration.files) - project.files(excludedDeps.flatMap {
                it.moduleArtifacts.map { artifact -> artifact.file }
            })
        } else {
            error("unexpected object $configuration")
        }
    }

     override fun resolve(configurations: Collection<FileCollection>): FileCollection {
        return configurations.map { resolve(it) }.fold(project.files()) { acc: FileCollection, element -> acc.plus(element) }
    }

    /**
     * Exclude dependencies that match the provided spec.
     */
    override fun exclude(spec: Spec<in ResolvedDependency>): DependencyFilter {
        excludeSpecs.add(spec)
        return this
    }

    /**
     * Include dependencies that match the provided spec.
     */
    override fun include(spec: Spec<in ResolvedDependency>): DependencyFilter {
        includeSpecs.add(spec)
        return this
    }

    /**
     * Create a spec that matches the provided project notation on group, name, and version
     */
    override fun project(notation: Map<String, Any>): Spec<in ResolvedDependency> {
        return dependency(project.dependencies.project(notation))
    }

    /**
     * Create a spec that matches the default configuration for the provided project path on group, name, and version
     */
     override fun project(notation: String): Spec<in ResolvedDependency> {
        return dependency(project.dependencies.project(mapOf(Pair("path", notation),
                                                             Pair("configuration", "default"))))
    }

    /**
     * Create a spec that matches dependencies using the provided notation on group, name, and version
     */
    override fun dependency(notation: Any): Spec<in ResolvedDependency> {
        return dependency(project.dependencies.create(notation))
    }

    /**
     * Create a spec that matches the provided dependency on group, name, and version
     */
    override fun dependency(dependency: Dependency): Spec<in ResolvedDependency> {
        val closure = closureOf<ResolvedDependency, Boolean> {
            (dependency.group?.isNotEmpty() == false || this.moduleGroup.matches(dependency.group!!.toRegex())) &&
            (dependency.name.isEmpty()               || this.moduleName.matches(dependency.name.toRegex()))     &&
            (dependency.version?.isEmpty() == false  || this.moduleVersion.matches(dependency.version!!.toRegex()))
        }

        return dependency(closure)
    }

    /**
     * Create a spec that matches the provided closure
     */
    override fun dependency(spec: Closure<Any?>): Spec<in ResolvedDependency> {
        return Specs.convertClosureToSpec(spec)
    }

    protected fun isIncluded(dependency: ResolvedDependency): Boolean {
        val included = includeSpecs.isEmpty() || includeSpecs.any { it.isSatisfiedBy(dependency) }
        val excluded = excludeSpecs.isNotEmpty() && excludeSpecs.any { it.isSatisfiedBy(dependency) }
        return included && !excluded
    }
}
