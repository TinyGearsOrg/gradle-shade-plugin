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

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.java.archives.Attributes
import org.gradle.api.java.archives.Manifest
import org.gradle.api.java.archives.ManifestMergeSpec
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.api.java.archives.internal.DefaultManifestMergeSpec
import org.gradle.util.internal.ConfigureUtil
import java.io.Writer

class DefaultInheritManifest
    constructor(private val fileResolver: FileResolver): InheritManifest {

    private val inheritMergeSpecs: MutableList<DefaultManifestMergeSpec> = mutableListOf()
    private val internalManifest: Manifest = DefaultManifest(fileResolver)

    override fun inheritFrom(vararg inheritPaths: Any): InheritManifest {
        inheritFrom(inheritPaths, null)
        return this
    }

    override fun inheritFrom(inheritPaths: Any, closure: Closure<Any>?): InheritManifest {
        val mergeSpec = DefaultManifestMergeSpec()
        mergeSpec.from(inheritPaths)
        inheritMergeSpecs.add(mergeSpec)
        closure?.apply { ConfigureUtil.configure(closure, mergeSpec) }
        return this
    }

    override fun getAttributes(): Attributes {
        return internalManifest.attributes
    }

    override fun getSections(): MutableMap<String, Attributes> {
        return internalManifest.sections
    }

    override fun attributes(map: MutableMap<String, *>?): Manifest {
        internalManifest.attributes(map)
        return this
    }

    override fun attributes(map: MutableMap<String, *>?, s: String?): Manifest {
        internalManifest.attributes(map, s)
        return this
    }

    override fun getEffectiveManifest(): Manifest {
        var base = DefaultManifest(fileResolver)
        inheritMergeSpecs.forEach {
            base = it.merge(base, fileResolver)
        }
        base.from(internalManifest)
        return base.effectiveManifest
    }

    fun writeTo(writer: Writer): Manifest {
        effectiveManifest.writeTo(writer as Any)
        return this
    }

    override fun writeTo(path: Any?): Manifest {
        effectiveManifest.writeTo(path)
        return this
    }

    override fun from(vararg mergePath: Any?): Manifest {
        internalManifest.from(mergePath)
        return this
    }

    override fun from(mergePath: Any?, closure: Closure<*>?): Manifest {
        internalManifest.from(mergePath, closure)
        return this
    }

    override fun from(mergePath: Any?, action: Action<ManifestMergeSpec>?): Manifest {
        internalManifest.from(mergePath, action)
        return this
    }
}
