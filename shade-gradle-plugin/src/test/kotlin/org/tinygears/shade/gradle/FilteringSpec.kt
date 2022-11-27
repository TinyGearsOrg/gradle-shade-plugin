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
package org.tinygears.shade.gradle

import org.junit.jupiter.api.BeforeEach
import kotlin.test.Test

class FilteringSpec: PluginSpecification() {

    @BeforeEach
    override fun setup() {
        super.setup()

        repo.module("shade", "a", "1.0")
            .insertFile("a.properties", "a")
            .insertFile("a2.properties", "a2")
            .publish()

        repo.module("shade", "b", "1.0")
            .insertFile("b.properties", "b")
            .publish()

        buildFile.appendText(
            """
            dependencies {
               implementation 'shade:a:1.0'
               implementation 'shade:b:1.0'
            }
            
            """.trimIndent()
        )
    }

    @Test
    fun `include all dependencies`() {
        run("shadeJar")
        contains(output, listOf("a.properties", "a2.properties", "b.properties"))
    }

    @Test
    fun `exclude files`() {
        buildFile.appendText(
            """
            // tag::excludeFile[]
            shadeJar {
               exclude 'a2.properties'
            }
            // end::excludeFile[]
            """.trimIndent()
        )

        run("shadeJar")

        contains(output, listOf("a.properties", "b.properties"))
        doesNotContain(output, listOf("a2.properties"))
    }

    @Test
    fun `exclude dependency`() {
        repo.module("shade", "c", "1.0")
            .insertFile("c.properties", "c")
            .publish()
        repo.module("shade", "d", "1.0")
            .insertFile("d.properties", "d")
            .dependsOn("c")
            .publish()

        buildFile.appendText(
            """
            // tag::excludeDep[]
            dependencies {
                implementation "shade:d:1.0"
            }

            shadeJar {
                dependencies {
                    exclude(dependency("shade:d:1.0"))
                }
            }
            // end::excludeDep[]
            """.trimIndent()
        )

        run("shadeJar")

        contains(output, listOf("a.properties", "a2.properties", "b.properties", "c.properties"))
        doesNotContain(output, listOf("d.properties"))
    }

    @Test
    fun `exclude dependency using wildcard syntax`() {
        repo.module("shade", "c", "1.0")
            .insertFile("c.properties", "c")
            .publish()
        repo.module("shade", "d", "1.0")
            .insertFile("d.properties", "d")
            .dependsOn("c")
            .publish()

        buildFile.appendText(
            """
            // tag::excludeDepWildcard[]
            dependencies {
                implementation "shade:d:1.0"
            }

            shadeJar {
                dependencies {
                    exclude(dependency("shade:d:.*"))
                }
            }
            // end::excludeDepWildcard[]
            """.trimIndent()
        )

        run("shadeJar")

        contains(output, listOf("a.properties", "a2.properties", "b.properties", "c.properties"))
        doesNotContain(output, listOf("d.properties"))
    }

//    @Issue("SHADOW-54")
//    @Ignore("TODO - need to figure out the test pollution here")
//    def "dependency exclusions affect UP-TO-DATE check"() {
//        given:
//        repo.module("shadow", "c", "1.0")
//            .insertFile("c.properties", "c")
//            .publish()
//        repo.module("shadow", "d", "1.0")
//            .insertFile("d.properties", "d")
//            .dependsOn("c")
//            .publish()
//
//        buildFile << """
//        dependencies {
//            implementation "shadow:d:1.0"
//        }
//
//        shadowJar {
//            dependencies {
//                exclude(dependency("shadow:d:1.0"))
//            }
//        }
//        """.stripIndent()
//
//        when:
//        run("shadowJar")
//
//        then:
//        contains(output, ["a.properties", "a2.properties", "b.properties", "c.properties"])
//
//        and:
//        doesNotContain(output, ["d.properties"])
//
//        when: "Update build file shadowJar dependency exclusion"
//        buildFile.text = buildFile.text.replace("exclude(dependency(\"shadow:d:1.0\"))",
//            "exclude(dependency(\"shadow:c:1.0\"))")
//
//        BuildResult result = run("shadowJar")
//
//        then:
//        assert result.task(":shadowJar").outcome == TaskOutcome.SUCCESS
//
//        and:
//        contains(output, ["a.properties", "a2.properties", "b.properties", "d.properties"])
//
//        and:
//        doesNotContain(output, ["c.properties"])
//    }

//    @Issue("SHADOW-62")
//    @Ignore
//    def "project exclusions affect UP-TO-DATE check"() {
//        given:
//        repo.module("shadow", "c", "1.0")
//            .insertFile("c.properties", "c")
//            .publish()
//        repo.module("shadow", "d", "1.0")
//            .insertFile("d.properties", "d")
//            .dependsOn("c")
//            .publish()
//
//        buildFile << """
//        dependencies {
//            implementation "shadow:d:1.0"
//        }
//
//        shadowJar {
//            dependencies {
//                exclude(dependency("shadow:d:1.0"))
//            }
//        }
//        """.stripIndent()
//
//        when:
//        run("shadowJar")
//
//        then:
//        contains(output, ["a.properties", "a2.properties", "b.properties", "c.properties"])
//
//        and:
//        doesNotContain(output, ["d.properties"])
//
//        when: "Update build file shadowJar dependency exclusion"
//        buildFile.text << """
//        shadowJar {
//            exclude "a.properties"
//        }
//        """.stripIndent()
//
//        BuildResult result = run("shadowJar")
//
//        then:
//        assert result.task(":shadowJar").outcome == TaskOutcome.SUCCESS
//
//        and:
//        contains(output, ["a2.properties", "b.properties", "d.properties"])
//
//        and:
//        doesNotContain(output, ["a.properties", "c.properties"])
//    }

    @Test
    fun `include dependency, excluding all others`() {
        repo.module("shade", "c", "1.0")
            .insertFile("c.properties", "c")
            .publish()
        repo.module("shade", "d", "1.0")
            .insertFile("d.properties", "d")
            .dependsOn("c")
            .publish()

        file("src/main/java/shade/Passed.java").appendText(
            """
            package shade;
            public class Passed {}
            """.trimIndent()
        )

        buildFile.appendText(
            """
            dependencies {
                implementation "shade:d:1.0"
            }

            shadeJar {
                dependencies {
                    include(dependency("shade:d:1.0"))
                }
            }
            """.trimIndent()
        )

        run("shadeJar")

        contains(output, listOf("d.properties", "shade/Passed.class"))
        doesNotContain(output, listOf("a.properties", "a2.properties", "b.properties", "c.properties"))
    }

    @Test
    fun `filter project dependencies`() {
        buildFile.writeText("")

        file("settings.gradle").appendText(
            """
            include "client", "server"
            """.trimIndent()
        )

        file("client/src/main/java/client/Client.java").appendText(
            """
            package client;
            public class Client {}
            """.trimIndent()
        )

        file("client/build.gradle").appendText(
            """
            ${getDefaultBuildScript()}
            dependencies { implementation "junit:junit:3.8.2" }
            """.trimIndent()
        )

        file("server/src/main/java/server/Server.java").appendText(
            """
            package server;
            import client.Client;
            public class Server {}
            """.trimIndent()
        )

        file("server/build.gradle").appendText(
            """
            ${getDefaultBuildScript()}

            // tag::excludeProject[]
            dependencies {
              implementation project(":client")
            }

            shadeJar {
               dependencies {
                   exclude(project(":client"))
               }
            }
            // end::excludeProject[]
            """.trimIndent()
        )

        val serverOutput = getFile("server/build/libs/server-1.0-all.jar")

        run(":server:shadeJar")

        serverOutput.exists()
        doesNotContain(serverOutput, listOf("client/Client.class"))
        contains(serverOutput, listOf("server/Server.class", "junit/framework/Test.class"))
    }

    @Test
    fun `exclude a transitive project dependency`() {
        buildFile.writeText("")

        file("settings.gradle").appendText(
            """
            include "client", "server"
            """.trimIndent()
        )

        file("client/src/main/java/client/Client.java").appendText(
            """
            package client;
            public class Client {}
            """.trimIndent()
        )

        file("client/build.gradle").appendText(
            """
            ${getDefaultBuildScript()}
            dependencies { implementation "junit:junit:3.8.2" }
            """.trimIndent()
        )

        file("server/src/main/java/server/Server.java").appendText(
            """
            package server;
            import client.Client;
            public class Server {}
            """.trimIndent()
        )

        file("server/build.gradle").appendText(
            """
            ${getDefaultBuildScript()}
            dependencies { implementation project(":client") }

            // tag::excludeSpec[]
            shadeJar {
               dependencies {
                   exclude(dependency {
                       it.moduleGroup == "junit"
                   })
               }
            }
            // end::excludeSpec[]
            """.trimIndent()
        )

        val serverOutput = getFile("server/build/libs/server-1.0-all.jar")

        run(":server:shadeJar")

        serverOutput.exists()
        doesNotContain(serverOutput, listOf("junit/framework/Test.class"))
        contains(serverOutput, listOf("client/Client.class", "server/Server.class"))
    }

    //http://mail-archives.apache.org/mod_mbox/ant-user/200506.mbox/%3C001d01c57756$6dc35da0$dc00a8c0@CTEGDOMAIN.COM%3E
    @Test
    fun `verify exclude precedence over include`() {
        buildFile.appendText(
            """
            // tag::excludeOverInclude[]
            shadeJar {
               include "*.jar"
               include "*.properties"
               exclude "a2.properties"
            }
            // end::excludeOverInclude[]
            """.trimIndent()
        )

        run("shadeJar")

        contains(output, listOf("a.properties", "b.properties"))
        doesNotContain(output, listOf("a2.properties"))
    }

    @Test
    fun `handle exclude with circular dependency`() {
        repo.module("shade", "c", "1.0")
            .insertFile("c.properties", "c")
            .dependsOn("d")
            .publish()
        repo.module("shade", "d", "1.0")
            .insertFile("d.properties", "d")
            .dependsOn("c")
            .publish()

        buildFile.appendText(
            """
            dependencies {
                implementation "shade:d:1.0"
            }

            shadeJar {
                dependencies {
                    exclude(dependency("shade:d:1.0"))
                }
            }
            """.trimIndent()
        )

        run("shadeJar")

        contains(output, listOf("a.properties", "a2.properties", "b.properties", "c.properties"))
        doesNotContain(output, listOf("d.properties"))
    }
}