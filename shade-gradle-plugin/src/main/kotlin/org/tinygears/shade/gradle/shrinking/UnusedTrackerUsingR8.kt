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

import com.android.tools.r8.*
import com.android.tools.r8.origin.Origin
import org.gradle.api.Project
import org.gradle.api.file.FileCollection
import org.slf4j.LoggerFactory
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

/**
 * An implementation of an `UnusedTracker` using R8, the optimization and shrinking tool
 * of the Android project.
 *
 * Contrary to the `UnusedTrackerUsingJDependency` this `UnusedTracker` is able to
 * fully remove unused fields / methods, leading to much better results for complex dependencies
 * like e.g. guava.
 */
class UnusedTrackerUsingR8
    private constructor(private val tmpDir:        Path,
                        private val configuration: R8Configuration,
                                    classDirs:     Iterable<File>,
                                    classJars:     FileCollection,
                                    toMinimize:    FileCollection): UnusedTracker(toMinimize) {

    private val projectFiles:  MutableList<File> = mutableListOf()
    private val dependencies:  MutableList<File> = mutableListOf()

    init {
        for (dir in classDirs) {
            val path = Paths.get(dir.absolutePath)
            collectClassFiles(path, projectFiles)
        }

        projectFiles.addAll(classJars.files)
    }

    override fun getPathToProcessedClass(classname: String): Path? {
        val className = removeExtension(classname).replace('/', '.')
        return Paths.get(tmpDir.toString(), className.replace("\\.".toRegex(), "/") + ".class")
    }

    override fun findUnused(): Set<String> {
        // We effectively run R8 twice:
        //  * first time disabling any processing, to retrieve all project classes
        //  * second time with a full shrink run to get all unused classes
        val builder = R8Command.builder(GradleDiagnosticHandler(true))
        populateBuilderWithProjectFiles(builder)

        // add all dependency jars
        for (dep in dependencies) {
            val path = Paths.get(dep.absolutePath)
            builder.addProgramFiles(path)
        }

        val removedClasses: MutableSet<String> = HashSet()

        // Add any class from the usage list to the list of removed classes.
        // This is a bit of a hack but the best things I could think of so far.
        builder.setProguardUsageConsumer(object : StringConsumer {
            private val LINE_SEPARATOR = System.getProperty("line.separator")
            private var lastString = LINE_SEPARATOR
            private var classString: String? = null
            private var expectingSeparator = false
            override fun accept(s: String, handler: DiagnosticsHandler) {
                if (classString == null && lastString == LINE_SEPARATOR) {
                    classString = s
                    expectingSeparator = true
                } else if (expectingSeparator && s == LINE_SEPARATOR) {
                    removedClasses.add(classString!!)
                    classString = null
                    expectingSeparator = false
                } else {
                    classString = null
                    expectingSeparator = false
                }
                lastString = s
            }
        })

        val configurationFiles = configuration.getConfigurationFiles().map { it.toPath() }.toTypedArray()
        if (configurationFiles.isNotEmpty()) {
            builder.addProguardConfigurationFiles(*configurationFiles)
        }

        val proguardConfig: MutableList<String> = generateKeepRules()
        proguardConfig.add("-dontoptimize")
        proguardConfig.add("-dontobfuscate")
        proguardConfig.add("-ignorewarnings")
        proguardConfig.addAll(configuration.getRules())
        builder.addProguardConfiguration(proguardConfig, Origin.unknown())

        // Set compilation mode to debug to disable any code of instruction
        // level optimizations.
        builder.mode = CompilationMode.DEBUG
        builder.programConsumer = object : ClassFileConsumer {
            override fun accept(byteDataView: ByteDataView, s: String, diagnosticsHandler: DiagnosticsHandler) {
                val name = typeNameToExternalClassName(s)

                // any class that is actually going to be written to the output
                // must not be present in the set of removed classes.
                // Should not really be needed, but we prefer to be paranoid.
                removedClasses.remove(name)
                val classFile = Paths.get(tmpDir.toString(), externalClassNameToInternal(name) + ".class")

                Files.createDirectories(classFile.parent)
                Files.write(classFile, byteDataView.buffer)
            }

            override fun finished(diagnosticsHandler: DiagnosticsHandler) {}
        }

        R8.run(builder.build())
        return removedClasses
    }

    /**
     * Returns a collection of necessary keep rules. For this purpose, R8 is executed
     * in a kind of pass through mode where we collect all classes that should
     * be kept as is (classes from each source set and api dependencies).
     *
     * This could be achieved differently and more efficiently, but is done like
     * that for convenience reasons atm.
     */
    private fun generateKeepRules(): MutableList<String> {
        val builder = R8Command.builder(GradleDiagnosticHandler(false))
        populateBuilderWithProjectFiles(builder)

        // add all dependencies as library jars to avoid warnings
        for (dep in dependencies) {
            val path = Paths.get(dep.absolutePath)
            builder.addLibraryFiles(path)
        }

        // disable everything, we just want to get a list of
        // all project classes.
        val configs: MutableList<String> = ArrayList()
        configs.add("-dontshrink")
        configs.add("-dontoptimize")
        configs.add("-dontobfuscate")
        configs.add("-ignorewarnings")
        configs.add("-dontwarn")
        builder.addProguardConfiguration(configs, Origin.unknown())

        // Set compilation mode to debug to disable any code of instruction
        // level optimizations.
        builder.mode = CompilationMode.DEBUG
        val keepRules: MutableList<String> = ArrayList()

        builder.programConsumer = object : ClassFileConsumer {
            override fun accept(byteDataView: ByteDataView, s: String, diagnosticsHandler: DiagnosticsHandler) {
                val name = typeNameToExternalClassName(s)
                keepRules.add("-keep,includedescriptorclasses class $name { *; }")
            }

            override fun finished(diagnosticsHandler: DiagnosticsHandler) {}
        }

        R8.run(builder.build())
        return keepRules
    }

    private fun populateBuilderWithProjectFiles(builder: R8Command.Builder) {
        addJDKLibrary(builder)
        for (f in projectFiles) {
            val path = Paths.get(f.absolutePath)
            if (f.absolutePath.endsWith(".class")) {
                val bytes = Files.readAllBytes(Paths.get(f.absolutePath))
                builder.addClassProgramData(bytes, Origin.unknown())
            } else {
                builder.addProgramFiles(path)
            }
        }
    }

    private fun collectClassFiles(dir: Path, result: MutableCollection<File>) {
        val file = dir.toFile()
        if (file.exists()) {
            val files = file.listFiles()
            if (files != null) {
                for (child in files) {
                    if (child.isDirectory) {
                        collectClassFiles(child.toPath(), result)
                    } else {
                        val relative = child.toPath()
                        if (isClassFile(relative)) {
                            result.add(child.absoluteFile)
                        }
                    }
                }
            }
        }
    }

    override fun addDependency(jarOrDir: File) {
        if (toMinimize.contains(jarOrDir)) {
            dependencies.add(jarOrDir)
        }
    }

    private class GradleDiagnosticHandler constructor(private val enableInfo: Boolean) : DiagnosticsHandler {
        override fun error(error: Diagnostic) {
            if (logger.isErrorEnabled) {
                DiagnosticsHandler.printDiagnosticToStream(error, "Error", System.err)
            }
        }

        override fun warning(warning: Diagnostic) {
            if (logger.isWarnEnabled) {
                DiagnosticsHandler.printDiagnosticToStream(warning, "Warning", System.out)
            }
        }

        override fun info(info: Diagnostic) {
            if (logger.isInfoEnabled && enableInfo) {
                DiagnosticsHandler.printDiagnosticToStream(info, "Info", System.out)
            }
        }

        companion object {
            private val logger = LoggerFactory.getLogger(GradleDiagnosticHandler::class.java)
        }
    }

    companion object {
        private const val TMP_DIR = "tmp/shadowJar/minimize"

        private fun typeNameToExternalClassName(typeName: String): String {
            val className = if (typeName.startsWith("L") && typeName.endsWith(";")) typeName.substring(1, typeName.length - 1) else typeName
            return className.replace("/".toRegex(), ".")
        }

        private fun externalClassNameToInternal(className: String): String {
            return className.replace("\\.".toRegex(), "/")
        }

        private fun removeExtension(fileName: String): String {
            val lastIndex = fileName.lastIndexOf('.')
            if (lastIndex != -1) {
                return fileName.substring(0, lastIndex)
            }
            return fileName
        }

        private fun isClassFile(path: Path): Boolean {
            val name = path.fileName.toString().lowercase(Locale.getDefault())
            return name != "module-info.class" && name.endsWith(".class")
        }

        private fun addJDKLibrary(builder: R8Command.Builder) {
            var javaHome = System.getProperty("java.home")
            if (javaHome == null) {
                javaHome = System.getenv("JAVA_HOME")
            }
            if (javaHome == null) {
                throw RuntimeException("unable to determine 'java.home' environment variable")
            }
            builder.addLibraryResourceProvider(JdkClassFileProvider.fromJdkHome(Paths.get(javaHome)))
        }

        fun forProject(project:               Project,
                       configuration:         R8Configuration,
                       apiJars:               FileCollection,
                       sourceSetsClassesDirs: Iterable<File>,
                       toMinimize:            FileCollection): UnusedTrackerUsingR8 {
            val tmpDir = Paths.get(project.buildDir.absolutePath, TMP_DIR)
            Files.createDirectories(tmpDir)
            return UnusedTrackerUsingR8(tmpDir, configuration, sourceSetsClassesDirs, apiJars, toMinimize)
        }
    }
}