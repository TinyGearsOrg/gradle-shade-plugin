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
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.DefaultCopySpec.DefaultCopySpecResolver
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.util.PatternSet
import org.tinygears.shade.gradle.ShadeStats
import org.tinygears.shade.gradle.relocation.PatternRelocator
import org.tinygears.shade.gradle.relocation.Relocator
import org.tinygears.shade.gradle.shrinking.*
import org.tinygears.shade.gradle.transformation.Transformer
import org.tinygears.shade.gradle.util.*
import java.util.concurrent.Callable

@CacheableTask
open class ShadeJar: Jar(), ShadeSpec {
    private var transformers: MutableList<Transformer> = mutableListOf()
    private var relocators:   MutableList<Relocator>   = mutableListOf()

    @get:Optional
    @get:Classpath
    var configurations: MutableList<FileCollection> = mutableListOf()

    @Transient
    private var dependencyFilter: DependencyFilter = DefaultDependencyFilter(project)

    private var useR8 = false

    private var r8Configuration: R8Configuration? = null

    private var minimizeJar = false

    @Transient
    private val dependencyFilterForMinimize: DependencyFilter = MinimizeDependencyFilter(project)

    @get:Classpath
    val toMinimize: FileCollection by lazy {
        if (minimizeJar) {
            dependencyFilterForMinimize.resolve(configurations).minus(apiJars)
        } else {
            project.objects.fileCollection()
        }
    }

    @get:Classpath
    val apiJars: FileCollection by lazy {
        if (minimizeJar) {
            UnusedTracker.getApiJarsFromProject(project)
        } else {
            project.objects.fileCollection()
        }
    }

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFiles
    val sourceSetsClassesDirs: FileCollection by lazy {
        val allClassesDirs = project.objects.fileCollection()
        if (minimizeJar) {
            for (sourceSet in project.extensions.getByType(SourceSetContainer::class.java)) {
                // do not include test sources
                if (sourceSet.name != SourceSet.TEST_SOURCE_SET_NAME) {
                    val classesDirs = sourceSet.output.classesDirs
                    allClassesDirs.from(classesDirs)
                }
            }
        }
        allClassesDirs.filter { file -> file.isDirectory }
    }

    private val shadowStats: ShadeStats = ShadeStats()

    @get:Classpath
    val includedDependencies: FileCollection
        get() = project.files(Callable { dependencyFilter.resolve(configurations) })

    init {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE // shadow filters out files later. This was the default behavior in  Gradle < 6.x
        manifest = DefaultInheritManifest(services[FileResolver::class.java])

        this.inputs.property("minimize", Callable { minimizeJar })
        this.outputs.doNotCacheIf("Has one or more transforms or relocators that are not cacheable", Spec {
//            for (transformer in transformers) {
//                if (!isCacheableTransform(transformer.getClass())) {
//                    return@Spec true
//                }
//            }

            for (relocator in relocators) {
                if (!isCacheable(relocator::class.java)) {
                    return@Spec true
                }
            }

            false
        })
    }

    override fun useR8(): ShadeSpec {
        useR8 = true
        r8Configuration = DefaultR8Configuration()
        return this
    }

    override fun useR8(configure: Action<R8Configuration>?): ShadeSpec {
        useR8()
        configure?.execute(r8Configuration!!)
        return this
    }

    override fun minimize(): ShadeSpec {
        minimizeJar = true
        return this
    }

    override fun minimize(configureClosure: Action<DependencyFilter>?): ShadeSpec {
        minimize()
        configureClosure?.execute(dependencyFilterForMinimize)
        return this
    }

    @get:Internal
    override val stats: ShadeStats
        get() = shadowStats

    override fun getManifest(): InheritManifest {
        return super.getManifest() as InheritManifest
    }

    override fun createCopyAction(): CopyAction {
        val documentationRegistry = services.get(DocumentationRegistry::class.java)
        val unusedTracker: UnusedTracker? = if (minimizeJar) {
            if (useR8) {
                UnusedTrackerUsingR8.forProject(project, r8Configuration!!, apiJars, sourceSetsClassesDirs.files, toMinimize)
            } else {
                UnusedTrackerUsingJDependency.forProject(apiJars, sourceSetsClassesDirs.files, toMinimize)
            }
        } else {
            null
        }

        return ShadeCopyAction(archiveFile.get().asFile, internalCompressor, documentationRegistry,
                               metadataCharset, transformers, relocators, rootPatternSet, shadowStats,
                               isPreserveFileTimestamps, minimizeJar, unusedTracker)
    }

    @get:Internal
    protected val internalCompressor: ZipCompressor
        get() = getInternalCompressor(entryCompression, this)

    @TaskAction
    override fun copy() {
        from(includedDependencies)
        super.copy()
        logger.info(shadowStats.toString())
    }

    /**
     * Utility method for assisting between changes in Gradle 1.12 and 2.x.
     *
     * @return this
     */
    @get:Internal
    protected val rootPatternSet: PatternSet
        get() {
            val resolver = mainSpec.buildRootResolver()
            if (resolver is DefaultCopySpecResolver) {
                return resolver.patternSet
            } else {
                error("unexpected")
            }
        }

    /**
     * Configure inclusion/exclusion of module and project dependencies into uber jar.
     *
     * @param configure the configuration of the filter
     * @return this
     */
    override fun dependencies(configure: Action<DependencyFilter>?): ShadeJar {
        configure?.execute(dependencyFilter)
        return this
    }

//    /**
//     * Add a Transformer instance for modifying JAR resources and configure.
//     *
//     * @param clazz the transformer to add. Must have a no-arg constructor
//     * @return this
//     */
//    @Throws(InstantiationException::class, IllegalAccessException::class, NoSuchMethodException::class, InvocationTargetException::class)
//    fun transform(clazz: Class<out Transformer?>): ShadowJar {
//        return transform(clazz, null)
//    }

//    /**
//     * Add a Transformer instance for modifying JAR resources and configure.
//     *
//     * @param clazz the transformer class to add. Must have no-arg constructor
//     * @param c the configuration for the transformer
//     * @return this
//     */
//    @Throws(InstantiationException::class, IllegalAccessException::class, NoSuchMethodException::class, InvocationTargetException::class)
//    fun <T : Transformer?> transform(clazz: Class<T?>, c: Action<T>?): ShadowJar {
//        val transformer: T = clazz.getDeclaredConstructor().newInstance()
//        addTransform(transformer, c)
//        return this
//    }
//
//    private fun isCacheableTransform(clazz: Class<out Transformer?>): Boolean {
//        return clazz.isAnnotationPresent(CacheableTransformer::class.java)
//    }

//    /**
//     * Add a preconfigured transformer instance.
//     *
//     * @param transformer the transformer instance to add
//     * @return this
//     */
//    fun transform(transformer: Transformer): ShadowJar {
//        addTransform<Transformer>(transformer, null)
//        return this
//    }
//
//    private fun <T : Transformer?> addTransform(transformer: T, c: Action<T>?) {
//        c?.execute(transformer)
//        transformers.add(transformer)
//    }

//    /**
//     * Syntactic sugar for merging service files in JARs.
//     *
//     * @return this
//     */
//    override fun mergeServiceFiles(): ShadowJar {
//        try {
//            transform(ServiceFileTransformer::class.java)
//        } catch (e: IllegalAccessException) {
//        } catch (e: InstantiationException) {
//        } catch (e: NoSuchMethodException) {
//        } catch (e: InvocationTargetException) {
//        }
//        return this
//    }
//
//    /**
//     * Syntactic sugar for merging service files in JARs.
//     *
//     * @return this
//     */
//    override fun mergeServiceFiles(rootPath: String?): ShadowJar {
//        try {
//            transform(ServiceFileTransformer::class.java, Action<Any> { serviceFileTransformer -> serviceFileTransformer.setPath(rootPath) })
//        } catch (e: IllegalAccessException) {
//        } catch (e: InstantiationException) {
//        } catch (e: NoSuchMethodException) {
//        } catch (e: InvocationTargetException) {
//        }
//        return this
//    }
//
//    /**
//     * Syntactic sugar for merging service files in JARs.
//     *
//     * @return this
//     */
//    fun mergeServiceFiles(configureClosure: Action<ServiceFileTransformer>?): ShadowJar {
//        try {
//            transform(ServiceFileTransformer::class.java, configureClosure)
//        } catch (e: IllegalAccessException) {
//        } catch (e: InstantiationException) {
//        } catch (e: NoSuchMethodException) {
//        } catch (e: InvocationTargetException) {
//        }
//        return this
//    }
//
//    /**
//     * Syntactic sugar for merging Groovy extension module descriptor files in JARs
//     */
//    override fun mergeGroovyExtensionModules(): ShadowJar {
//        try {
//            transform(GroovyExtensionModuleTransformer::class.java)
//        } catch (e: IllegalAccessException) {
//        } catch (e: InstantiationException) {
//        } catch (e: NoSuchMethodException) {
//        } catch (e: InvocationTargetException) {
//        }
//        return this
//    }

//    /**
//     * Syntax sugar for merging service files in JARs
//     */
//    override fun append(resourcePath: String?): ShadowJar {
//        try {
//            transform(AppendingTransformer::class.java, Action<Any> { transformer -> transformer.setResource(resourcePath) })
//        } catch (e: IllegalAccessException) {
//        } catch (e: InstantiationException) {
//        } catch (e: NoSuchMethodException) {
//        } catch (e: InvocationTargetException) {
//        }
//        return this
//    }

    /**
     * Add a class relocator that maps each class in the pattern to the provided destination.
     *
     * @param pattern the source pattern to relocate
     * @param destination the destination package
     * @return this
     */
    override fun relocate(pattern: String, destination: String): ShadeJar {
        return relocate(pattern, destination, null)
    }

    /**
     * Add a class relocator that maps each class in the pattern to the provided destination.
     *
     * @param pattern the source pattern to relocate
     * @param destination the destination package
     * @param configure the configuration of the relocator
     * @return this
     */
    override fun relocate(pattern: String, destination: String, configure: Action<Relocator>?): ShadeJar {
        val relocator = PatternRelocator(pattern, destination, ArrayList(), ArrayList())
        addRelocator(relocator, configure)
        return this
    }

    /**
     * Add a relocator instance.
     *
     * @param relocator the relocator instance to add
     * @return this
     */
    override fun relocate(relocator: Relocator): ShadeJar {
        addRelocator(relocator, null)
        return this
    }

    /**
     * Add a relocator of the provided class.
     *
     * @param relocatorClass the relocator class to add. Must have a no-arg constructor.
     * @return this
     */
    override fun relocate(relocatorClass: Class<out Relocator>): ShadeJar {
        return relocate(relocatorClass, null)
    }

    private fun <R : Relocator> addRelocator(relocator: R, configure: Action<R>?) {
        configure?.execute(relocator)
        relocators.add(relocator)
    }

    /**
     * Add a relocator of the provided class and configure.
     *
     * @param relocatorClass the relocator class to add. Must have a no-arg constructor
     * @param configure the configuration for the relocator
     * @return this
     */
    override fun <R : Relocator> relocate(relocatorClass: Class<R>, configure: Action<R>?): ShadeJar {
        val relocator = relocatorClass.getDeclaredConstructor().newInstance()
        addRelocator(relocator, configure)
        return this
    }

    private fun isCacheable(clazz: Class<out Any?>): Boolean {
        return clazz.isAnnotationPresent(Cacheable::class.java)
    }

//    @Nested
//    fun getTransformers(): List<Transformer> {
//        return transformers
//    }
//
//    fun setTransformers(transformers: MutableList<Transformer>) {
//        this.transformers = transformers
//    }

//    @Nested
//    fun getRelocators(): List<Relocator> {
//        return relocators
//    }
//
//    fun setRelocators(relocators: MutableList<Relocator>) {
//        this.relocators = relocators
//    }
}
