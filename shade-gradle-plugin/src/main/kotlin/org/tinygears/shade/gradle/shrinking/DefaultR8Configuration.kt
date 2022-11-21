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

import java.io.File

class DefaultR8Configuration: R8Configuration {

    private val additionalRules:    MutableList<String> = mutableListOf()
    private val configurationFiles: MutableList<File>   = mutableListOf()

    override fun getRules(): Collection<String> {
        return additionalRules
    }

    override fun rule(rule: String) {
        additionalRules.add(rule)
    }

    override fun getConfigurationFiles(): Collection<File> {
        return configurationFiles
    }

    override fun configuration(configurationFile: File) {
        configurationFiles.add(configurationFile)
    }
}
