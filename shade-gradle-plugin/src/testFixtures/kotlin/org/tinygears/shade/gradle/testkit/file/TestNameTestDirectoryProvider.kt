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
package org.tinygears.shade.gradle.testkit.file

import org.apache.commons.lang3.StringUtils
//import org.junit.rules.MethodRule
//import org.junit.rules.TestRule
//import org.junit.runner.Description
//import org.junit.runners.model.FrameworkMethod
//import org.junit.runners.model.Statement
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

/**
 * A JUnit rule which provides a unique temporary folder for the test.
 */
class TestNameTestDirectoryProvider { //}: MethodRule, TestRule, TestDirectoryProvider {
    private var dir: TestFile? = null
    private var prefix: String? = null

    private fun determinePrefix(): String {
        val stackTrace = RuntimeException().stackTrace
        for (element in stackTrace) {
            if (element.className.endsWith("Test") || element.className.endsWith("Spec")) {
                return StringUtils.substringAfterLast(element.className, ".") + "/unknown-test-" + testCounter.getAndIncrement()
            }
        }
        return "unknown-test-class-" + testCounter.getAndIncrement()
    }

//    override fun apply(base: Statement, method: FrameworkMethod, target: Any): Statement {
//        init(method.name, target.javaClass.simpleName)
//        return object : Statement() {
//            @Throws(Throwable::class)
//            override fun evaluate() {
//                base.evaluate()
//                testDirectory.maybeDeleteDir()
//                // Don't delete on failure
//            }
//        }
//    }
//
//    override fun apply(base: Statement, description: Description): Statement {
//        init(description.methodName, description.testClass.simpleName)
//        return object : Statement() {
//            @Throws(Throwable::class)
//            override fun evaluate() {
//                base.evaluate()
//                testDirectory.maybeDeleteDir()
//                // Don't delete on failure
//            }
//        }
//    }

    private fun init(methodName: String, className: String) {
        var methodName: String? = methodName
        if (methodName == null) {
            // must be a @ClassRule; use the rule's class name instead
            methodName = javaClass.simpleName
        }
        if (prefix == null) {
            var safeMethodName = methodName!!.replace("\\s".toRegex(), "_").replace(File.pathSeparator, "_").replace(":", "_")
            if (safeMethodName.length > 64) {
                safeMethodName = safeMethodName.substring(0, 32) + "..." + safeMethodName.substring(safeMethodName.length - 32)
            }
            prefix = String.format("%s/%s", className, safeMethodName)
        }
    }

    // This can happen if this is used in a constructor or a @Before method. It also happens when using
    // @RunWith(SomeRunner) when the runner does not support rules.
//    override val testDirectory: TestFile
//        get() {
//            if (dir == null) {
//                if (prefix == null) {
//                    // This can happen if this is used in a constructor or a @Before method. It also happens when using
//                    // @RunWith(SomeRunner) when the runner does not support rules.
//                    prefix = determinePrefix()
//                }
//                var counter = 1
//                while (true) {
//                    dir = root!!.file((if (counter == 1) prefix else String.format("%s%d", prefix, counter))!!)
//                    if (dir!!.mkdirs()) {
//                        break
//                    }
//                    counter++
//                }
//            }
//            return dir!!
//        }

//    fun file(vararg path: String): TestFile {
//        return testDirectory.file(*path)
//    }

//    fun createFile(vararg path: String): TestFile {
//        return file(*path).createFile()
//    }
//
//    fun createDir(vararg path: String): TestFile {
//        return file(*path).createDir()
//    }

    companion object {
        private var root: TestFile? = null
        private val testCounter = AtomicInteger(1)

        init {
            // NOTE: the space in the directory name is intentional
            root = TestFile(File("build/tmp/test files"))
        }

        fun newInstance(): TestNameTestDirectoryProvider {
            return TestNameTestDirectoryProvider()
        }

//        fun newInstance(method: FrameworkMethod, target: Any): TestNameTestDirectoryProvider {
//            val testDirectoryProvider = TestNameTestDirectoryProvider()
//            testDirectoryProvider.init(method.name, target.javaClass.simpleName)
//            return testDirectoryProvider
//        }
    }
}