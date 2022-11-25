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
package org.tinygears.shade.gradle.testkit.repo.maven

class MavenScope constructor(val dependencies: MutableMap<String, MavenDependency> = mutableMapOf()) {
    fun assertDependsOn(expected: Array<String>) {
        assert(dependencies.size == expected.size)

        for (str in expected) {
            val key = str.substringBefore("@")
            val dependency = expectDependency(key)

            var type: String? = null
            if (str != key) {
                type = str.substringAfter("@")
            }
            assert(dependency.hasType(type))
        }
    }

    fun expectDependency(key: String): MavenDependency {
        return dependencies[key] ?: throw AssertionError("could not find expected dependency $key. Actual: ${dependencies.values}")
    }
}
