import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent.*
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    `maven-publish`
}

val versionCatalog = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

tasks.withType<Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        // set options for log level LIFECYCLE
        events = setOf(FAILED, PASSED, SKIPPED, STANDARD_OUT)
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true

        // set options for log level DEBUG and INFO
        debug {
            events = setOf(STARTED, FAILED, PASSED, SKIPPED, STANDARD_ERROR, STANDARD_OUT)
            exceptionFormat = TestExceptionFormat.FULL
        }
        info {
            events = debug.events
            exceptionFormat = debug.exceptionFormat
        }
    }

    addTestListener(object : TestListener {
        override fun beforeTest(desc: TestDescriptor) = Unit
        override fun beforeSuite(desc: TestDescriptor) = Unit
        override fun afterTest(desc: TestDescriptor, result: TestResult) = Unit
        override fun afterSuite(desc: TestDescriptor, result: TestResult) {
            printResults(desc, result)
        }
    })

    jvmArgs = jvmArgs?.plus(listOf("--add-opens=java.base/java.lang=ALL-UNNAMED"))
}

fun printResults(desc: TestDescriptor, result: TestResult) {
    if (desc.parent == null) {
        val output = result.run {
            "Results: $resultType (" +
                    "$testCount tests, " +
                    "$successfulTestCount successes, " +
                    "$failedTestCount failures, " +
                    "$skippedTestCount skipped" +
                    ")"
        }
        val testResultLine = "|  $output  |"
        val repeatLength = testResultLine.length
        val separationLine = "-".repeat(repeatLength)
        println(separationLine)
        println(testResultLine)
        println(separationLine)
    }
}

testing {
    suites {
        getByName("test", JvmTestSuite::class) {
            useJUnitJupiter(versionCatalog.findVersion("jupiter").get().requiredVersion)
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = Versions.JVM_TARGET
        freeCompilerArgs = freeCompilerArgs + listOf("-Xjvm-default=all", "-Xlambdas=indy")
    }
}

java {
    sourceCompatibility = JavaVersion.toVersion(Versions.JVM_TARGET)
    targetCompatibility = JavaVersion.toVersion(Versions.JVM_TARGET)
}

dependencies {
    testImplementation(kotlin("test"))
}