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

import org.tinygears.bat.classfile.ClassFile
import org.tinygears.bat.classfile.editor.ClassRenamer
import org.tinygears.bat.classfile.io.ClassFileReader
import org.tinygears.bat.classfile.io.ClassFileWriter
import org.tinygears.shade.gradle.ShadeStats
import org.tinygears.shade.gradle.relocation.RelocatorRemapper
import org.tinygears.shade.gradle.relocation.Relocator
import org.tinygears.shade.gradle.transformation.Transformer
import org.apache.tools.ant.taskdefs.Zip
import org.apache.tools.zip.*
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.file.FileCopyDetails
import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.DocumentationRegistry
import org.gradle.api.internal.file.CopyActionProcessingStreamAction
import org.gradle.api.internal.file.DefaultFileTreeElement
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.internal.file.copy.FileCopyDetailsInternal
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.api.tasks.util.PatternSet
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.tinygears.shade.gradle.shrinking.UnusedTracker
import org.tinygears.shade.gradle.transformation.TransformerContext
import org.tinygears.shade.gradle.util.ZipCompressor
import java.io.*
import java.nio.file.Files
import java.util.*
import java.util.zip.ZipException
import kotlin.io.path.inputStream

class ShadeCopyAction
    constructor(
        private val zipFile:                File,
        private val compressor:             ZipCompressor,
        private val documentationRegistry:  DocumentationRegistry,
        private val encoding:               String?,
        private val transformers:           List<Transformer>,
        private val relocators:             List<Relocator>,
        private val patternSet:             PatternSet,
        private val stats:                  ShadeStats,
        private val preserveFileTimestamps: Boolean,
        private val minimizeJar:            Boolean,
        private val unusedTracker:          UnusedTracker?) : CopyAction {

    override fun execute(stream: CopyActionProcessingStream): WorkResult {
        val unusedClasses: Set<String> = if (minimizeJar) {
            stream.process(object: BaseStreamAction() {
                override fun visitFile(fileDetails: FileCopyDetails) {
                    // All project sources are already present, we just need
                    // to deal with JAR dependencies.
                    if (isArchive(fileDetails)) {
                        unusedTracker!!.addDependency(fileDetails.file)
                    }
                }
            })
            unusedTracker!!.findUnused()
        } else {
            Collections.emptySet()
        }

        val zipOutputStream: ZipOutputStream

        try {
            zipOutputStream = compressor.createArchiveOutputStream(zipFile)
        } catch (e: Exception) {
            throw GradleException("Could not create ZIP '${zipFile}'", e)
        }

        try {
            withResource(zipOutputStream) { outputStream ->
                try {
                    stream.process(StreamAction(outputStream, encoding, transformers, relocators, patternSet, unusedClasses, stats))
                    processTransformers(outputStream)
                } catch (e: Exception) {
                    logger.error("ex", e)
                    // TODO this should not be rethrown
                    throw e
                }
            }
        } catch (e: IOException) {
            if (e.cause is Zip64RequiredException) {
                throw Zip64RequiredException(
                    "%s\n\nTo build this archive, please enable the zip64 extension.\nSee: %s".format(
                        e.cause?.message, documentationRegistry.getDslRefForProperty(Zip::class.java, "zip64"))
                    )
            }
        }
        return WorkResults.didWork(true)
    }

    private fun processTransformers(stream: ZipOutputStream) {
        transformers.forEach { transformer ->
            if (transformer.hasTransformedResource()) {
                transformer.modifyOutputStream(stream, preserveFileTimestamps)
            }
        }
    }

    private fun getArchiveTimeFor(timestamp: Long): Long {
        return if (preserveFileTimestamps) timestamp else CONSTANT_TIME_FOR_ZIP_ENTRIES
    }

    private fun setArchiveTimes(zipEntry: ZipEntry): ZipEntry {
        if (!preserveFileTimestamps) {
            zipEntry.time = CONSTANT_TIME_FOR_ZIP_ENTRIES
        }
        return zipEntry
    }

    companion object {
        val CONSTANT_TIME_FOR_ZIP_ENTRIES: Long = GregorianCalendar(1980, 1, 1, 0, 0, 0).timeInMillis

        private val logger: Logger = LoggerFactory.getLogger(ShadeCopyAction::class.java)

        private fun <T: Closeable> withResource(resource: T, action: Action<in T>) {
            try {
                action.execute(resource)
            } catch(t: Throwable) {
                try {
                    resource.close()
                } catch (e: IOException) {
                    // Ignored
                }
                throw t
            }

            resource.close()
        }
    }

    abstract inner class BaseStreamAction: CopyActionProcessingStreamAction {
        protected fun isArchive(fileDetails: FileCopyDetails): Boolean {
            return fileDetails.relativePath.pathString.endsWith(".jar")
        }

        protected fun isClass(fileDetails: FileCopyDetails): Boolean {
            return isClass(fileDetails.path)
        }

        protected fun isClass(path: String): Boolean {
            return path.endsWith(".class")
        }

        override fun processFile(details: FileCopyDetailsInternal) {
            if (details.isDirectory) {
                visitDir(details)
            } else {
                visitFile(details)
            }
        }

        protected open fun visitDir(dirDetails: FileCopyDetails) {}
        protected abstract fun visitFile(fileDetails: FileCopyDetails)
    }

    private inner class StreamAction
        constructor(
            private val zipOutputStream: ZipOutputStream,
                        encoding:        String?,
            private val transformers:    List<Transformer>,
                        relocators:      List<Relocator>,
            private val patternSet:      PatternSet,
            private val unusedClasses:   Set<String>,
            private val stats:           ShadeStats): BaseStreamAction() {

        private val remapper: RelocatorRemapper = RelocatorRemapper(relocators, stats)
        private val visitedFiles: MutableSet<String> = mutableSetOf()

        init {
            if (encoding != null) {
                zipOutputStream.encoding = encoding
            }
        }

        private fun recordVisit(path: RelativePath): Boolean {
            return visitedFiles.add(path.pathString)
        }

        override fun visitFile(fileDetails: FileCopyDetails) {
            if (!isArchive(fileDetails)) {
                try {
                    val isClass = isClass(fileDetails)
                    if (!remapper.hasRelocators() || !isClass) {
                        if (!isTransformable(fileDetails)) {
                            val mappedPath    = remapper.map(fileDetails.relativePath.pathString)
                            val archiveEntry  = ZipEntry(mappedPath)
                            archiveEntry.time = getArchiveTimeFor(fileDetails.lastModified)
                            archiveEntry.unixMode = (UnixStat.FILE_FLAG or fileDetails.mode)
                            zipOutputStream.putNextEntry(archiveEntry)
                            fileDetails.copyTo(zipOutputStream)
                            zipOutputStream.closeEntry()
                        } else {
                            transform(fileDetails)
                        }
                    } else if (isClass && !isUnused(fileDetails.path)) {
                        remapClass(fileDetails)
                    }
                    recordVisit(fileDetails.relativePath)
                } catch (e: Exception) {
                    throw GradleException("could not add %s to ZIP '%s'.".format(fileDetails, zipFile), e)
                }
            } else {
                processArchive(fileDetails)
            }
        }

        private fun processArchive(fileDetails: FileCopyDetails) {
            stats.startJar()
            val archive = ZipFile(fileDetails.file)
            archive.use {
                val archiveElements = archive.entries.asSequence().map { ArchiveFileTreeElement(RelativeArchivePath(it)) }.toList()
                val patternSpec: Spec<FileTreeElement> = patternSet.asSpec
                val filteredArchiveElements = archiveElements.filter { archiveElement ->
                    patternSpec.isSatisfiedBy(archiveElement.asFileTreeElement())
                }
                filteredArchiveElements.forEach { archiveElement ->
                    if (archiveElement.relativePath.isFile) {
                        visitArchiveFile(archiveElement, archive)
                    }
                }
            }
            stats.finishJar()
        }

        private fun visitArchiveDirectory(archiveDir: RelativeArchivePath) {
            if (recordVisit(archiveDir)) {
                zipOutputStream.putNextEntry(archiveDir.entry)
                zipOutputStream.closeEntry()
            }
        }

        private fun visitArchiveFile(archiveFile: ArchiveFileTreeElement, archive: ZipFile) {
            val archiveFilePath = archiveFile.relativePath
            if (archiveFile.isClassFile || !isTransformable(archiveFile)) {
                if (recordVisit(archiveFilePath) && !isUnused(archiveFilePath.entry.name)) {
                    if (!remapper.hasRelocators() || !archiveFile.isClassFile) {
                        copyArchiveEntry(archiveFilePath, archive)
                    } else {
                        remapClass(archiveFilePath, archive)
                    }
                }
            } else {
                transform(archiveFile, archive)
            }
        }

        private fun addParentDirectories(file: RelativeArchivePath?) {
            if (null != file) {
                addParentDirectories(file.parent)
                if (!file.isFile) {
                    visitArchiveDirectory(file)
                }
            }
        }

        private fun isUnused(classPath: String): Boolean {
            val dotIndex = classPath.lastIndexOf('.')
            val className = if (dotIndex != -1) {
                classPath.substring(0, dotIndex)
            } else {
                classPath
            }.replace('/', '.')

            val result = unusedClasses.contains(className)
            if (result) {
                logger.debug("dropping unused class: $className")
            }
            return result
        }

        private fun getClassInputStreamFromUnusedTracker(className: String): InputStream? {
            return if (unusedTracker != null) {
                val path = unusedTracker.getPathToProcessedClass(className)
                if (path != null && Files.exists(path)) {
                    path.inputStream()
                } else {
                    null
                }
            } else {
                null
            }
        }

        private fun remapClass(file: RelativeArchivePath, archive: ZipFile) {
            if (file.isClassFile) {
                val zipEntry = setArchiveTimes(ZipEntry(remapper.mapPath(file) + ".class"))
                addParentDirectories(RelativeArchivePath(zipEntry))

                val `is` =
                    getClassInputStreamFromUnusedTracker(file.entry.name) ?: archive.getInputStream(file.entry)

                `is`.use {
                    remapClass(it, file.pathString, file.entry.time)
                }
            }
        }

        private fun remapClass(fileCopyDetails: FileCopyDetails) {
            if (isClass(fileCopyDetails)) {
                val `is` =
                    getClassInputStreamFromUnusedTracker(fileCopyDetails.relativePath.pathString) ?: fileCopyDetails.file.inputStream()

                `is`.use {
                    remapClass(`is`, fileCopyDetails.path, fileCopyDetails.lastModified)
                }
            }
        }

        /**
         * Applies remapping to the given class with the specified relocation path.
         * The remapped class is then written to the zip file.
         */
        private fun remapClass(`is`: InputStream, path: String, lastModified: Long) {
            val classFile = ClassFile.empty()
            val reader    = ClassFileReader(`is`)
            reader.visitClassFile(classFile)

            val classRenamer = ClassRenamer(remapper)
            classRenamer.visitClassFile(classFile)

            // Temporarily remove the multi-release prefix.
            val multiReleasePrefixRegex = "^META-INF/versions/\\d+/".toRegex()
            val multiReleasePrefix = multiReleasePrefixRegex.find(path)?.value ?: ""
            val normalizedPath = path.replace(multiReleasePrefix, "")
            val mappedName = multiReleasePrefix + remapper.mapPath(normalizedPath) + ".class"

            try {
                // Now we put it back on so the class file is written out with the right extension.
                val archiveEntry = ZipEntry(mappedName)
                archiveEntry.time = getArchiveTimeFor(lastModified)
                zipOutputStream.putNextEntry(archiveEntry)

                val classFileWriter = ClassFileWriter(zipOutputStream)
                classFileWriter.visitClassFile(classFile)

                zipOutputStream.closeEntry()
            } catch (e: ZipException) {
                logger.warn("We have a duplicate $mappedName in source project")
            }
        }

        private fun copyArchiveEntry(archiveFile: RelativeArchivePath, archive: ZipFile) {
            val mappedPath = remapper.map(archiveFile.entry.name)
            val entry      = ZipEntry(mappedPath)
            entry.time = getArchiveTimeFor(archiveFile.entry.time)
            val mappedFile = RelativeArchivePath(entry)
            addParentDirectories(mappedFile)
            zipOutputStream.putNextEntry(mappedFile.entry)

            val `is` =
                (if (isClass(archiveFile.entry.name)) getClassInputStreamFromUnusedTracker(archiveFile.entry.name) else null) ?:
                archive.getInputStream(archiveFile.entry)

            `is`.use {
                it.copyTo(zipOutputStream)
            }

            zipOutputStream.closeEntry()
        }

        override fun visitDir(dirDetails: FileCopyDetails) {
            try {
                // Trailing slash in name indicates that entry is a directory
                val path = dirDetails.relativePath.pathString + '/'
                val archiveEntry = ZipEntry(path)
                archiveEntry.time = getArchiveTimeFor(dirDetails.lastModified)
                archiveEntry.unixMode = (UnixStat.DIR_FLAG or dirDetails.mode)
                zipOutputStream.putNextEntry(archiveEntry)
                zipOutputStream.closeEntry()
                recordVisit(dirDetails.relativePath)
            } catch (e: Exception) {
                throw GradleException("could not add %s to ZIP '%s'.".format(dirDetails, zipFile), e)
            }
        }

        private fun transform(element: ArchiveFileTreeElement, archive: ZipFile) {
            transformAndClose(element, archive.getInputStream(element.relativePath.entry))
        }

        private fun transform(details: FileCopyDetails) {
            transformAndClose(details, details.file.inputStream())
        }

        private fun transformAndClose(element: FileTreeElement, `is`: InputStream) {
            `is`.use {
                val mappedPath = remapper.map(element.relativePath.pathString)
                transformers.find { it.canTransformResource(element) }?.transform(
                    TransformerContext(mappedPath, `is`, relocators, stats)
                )
            }
        }

        private fun isTransformable(element: FileTreeElement): Boolean {
            return transformers.any { it.canTransformResource(element) }
        }
    }

    inner class RelativeArchivePath constructor(val entry: ZipEntry)
        : RelativePath(!entry.isDirectory, *entry.name.split("/").filter { it.isNotEmpty() }.toTypedArray()) {

        val isClassFile: Boolean
            get() = lastName?.endsWith(".class") ?: false

        override fun getParent(): RelativeArchivePath? {
            return if (segments.isEmpty() || segments.size == 1) {
                null
            } else {
                // Parent is always a directory so add / to the end of the path
                val path = buildString {
                    for (i in 0 until segments.size - 1) {
                        append(segments[i])
                        append('/')
                    }
                }

                RelativeArchivePath(setArchiveTimes(ZipEntry(path)))
            }
        }
    }

    class ArchiveFileTreeElement constructor(private val archivePath: RelativeArchivePath): FileTreeElement {
        val isClassFile: Boolean
            get() = archivePath.isClassFile

        override fun getFile(): File {
            error("should not be called")
        }

        override fun isDirectory(): Boolean {
            return archivePath.entry.isDirectory
        }

        override fun getLastModified(): Long {
            return archivePath.entry.lastModifiedDate.time
        }

        override fun getSize(): Long {
            return archivePath.entry.size
        }

        override fun open(): InputStream {
            error("should not be called")
        }

        override fun copyTo(output: OutputStream) {
            error("should not be called")
        }

        override fun copyTo(target: File): Boolean {
            return false
        }

        override fun getName(): String {
            return archivePath.lastName
        }

        override fun getPath(): String {
            return archivePath.pathString
        }

        override fun getRelativePath(): RelativeArchivePath {
            return archivePath
        }

        override fun getMode(): Int {
            return archivePath.entry.unixMode
        }

        fun asFileTreeElement(): FileTreeElement {
            return DefaultFileTreeElement(null, RelativePath(!isDirectory, *archivePath.segments), null, null)
        }
    }
}
