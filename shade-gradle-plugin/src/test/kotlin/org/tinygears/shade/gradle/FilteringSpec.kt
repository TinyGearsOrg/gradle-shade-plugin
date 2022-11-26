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

//    def "exclude dependency"() {
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
//        // tag::excludeDep[]
//        dependencies {
//            implementation "shadow:d:1.0"
//        }
//
//        shadowJar {
//            dependencies {
//                exclude(dependency("shadow:d:1.0"))
//            }
//        }
//        // end::excludeDep[]
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
//    }
//
//    @Issue("SHADOW-83")
//    def "exclude dependency using wildcard syntax"() {
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
//        // tag::excludeDepWildcard[]
//        dependencies {
//            implementation "shadow:d:1.0"
//        }
//
//        shadowJar {
//            dependencies {
//                exclude(dependency("shadow:d:.*"))
//            }
//        }
//        // end::excludeDepWildcard[]
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
//    }
//
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
//
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
//
//    def "include dependency, excluding all others"() {
//        given:
//        repo.module("shadow", "c", "1.0")
//            .insertFile("c.properties", "c")
//            .publish()
//        repo.module("shadow", "d", "1.0")
//            .insertFile("d.properties", "d")
//            .dependsOn("c")
//            .publish()
//
//        file("src/main/java/shadow/Passed.java") << """
//        package shadow;
//        public class Passed {}
//        """.stripIndent()
//
//        buildFile << """
//        dependencies {
//            implementation "shadow:d:1.0"
//        }
//
//        shadowJar {
//            dependencies {
//                include(dependency("shadow:d:1.0"))
//            }
//        }
//        """.stripIndent()
//
//        when:
//        run("shadowJar")
//
//        then:
//        contains(output, ["d.properties", "shadow/Passed.class"])
//
//        and:
//        doesNotContain(output, ["a.properties", "a2.properties", "b.properties", "c.properties"])
//    }
//
//    def "filter project dependencies"() {
//        given:
//        buildFile.text = ""
//
//        file("settings.gradle") << """
//            include "client", "server"
//        """.stripIndent()
//
//        file("client/src/main/java/client/Client.java") << """
//            package client;
//            public class Client {}
//        """.stripIndent()
//
//        file("client/build.gradle") << """
//            ${defaultBuildScript}
//            dependencies { implementation "junit:junit:3.8.2" }
//        """.stripIndent()
//
//        file("server/src/main/java/server/Server.java") << """
//            package server;
//            import client.Client;
//            public class Server {}
//        """.stripIndent()
//
//        file("server/build.gradle") << """
//            ${defaultBuildScript}
//
//            // tag::excludeProject[]
//            dependencies {
//              implementation project(":client")
//            }
//
//            shadowJar {
//               dependencies {
//                   exclude(project(":client"))
//               }
//            }
//            // end::excludeProject[]
//        """.stripIndent()
//
//        File serverOutput = getFile("server/build/libs/server-1.0-all.jar")
//
//        when:
//        run(":server:shadowJar")
//
//        then:
//        serverOutput.exists()
//        doesNotContain(serverOutput, [
//            "client/Client.class",
//        ])
//
//        and:
//        contains(serverOutput, ["server/Server.class", "junit/framework/Test.class"])
//    }
//
//    def "exclude a transitive project dependency"() {
//        given:
//        buildFile.text = ""
//
//        file("settings.gradle") << """
//            include "client", "server"
//        """.stripIndent()
//
//        file("client/src/main/java/client/Client.java") << """
//            package client;
//            public class Client {}
//        """.stripIndent()
//
//        file("client/build.gradle") << """
//            ${defaultBuildScript}
//            dependencies { implementation "junit:junit:3.8.2" }
//        """.stripIndent()
//
//        file("server/src/main/java/server/Server.java") << """
//            package server;
//            import client.Client;
//            public class Server {}
//        """.stripIndent()
//
//        file("server/build.gradle") << """
//            ${defaultBuildScript}
//            dependencies { implementation project(":client") }
//
//            // tag::excludeSpec[]
//            shadowJar {
//               dependencies {
//                   exclude(dependency {
//                       it.moduleGroup == "junit"
//                   })
//               }
//            }
//            // end::excludeSpec[]
//        """.stripIndent()
//
//        File serverOutput = getFile("server/build/libs/server-1.0-all.jar")
//
//        when:
//        run(":server:shadowJar")
//
//        then:
//        serverOutput.exists()
//        doesNotContain(serverOutput, [
//            "junit/framework/Test.class"
//        ])
//
//        and:
//        contains(serverOutput, [
//            "client/Client.class",
//            "server/Server.class"])
//    }
//
//    //http://mail-archives.apache.org/mod_mbox/ant-user/200506.mbox/%3C001d01c57756$6dc35da0$dc00a8c0@CTEGDOMAIN.COM%3E
//    def "verify exclude precedence over include"() {
//        given:
//        buildFile << """
//            // tag::excludeOverInclude[]
//            shadowJar {
//               include "*.jar"
//               include "*.properties"
//               exclude "a2.properties"
//            }
//            // end::excludeOverInclude[]
//        """.stripIndent()
//
//        when:
//        run("shadowJar")
//
//        then:
//        contains(output, ["a.properties", "b.properties"])
//
//        and:
//        doesNotContain(output, ["a2.properties"])
//    }
//
//    @Issue("SHADOW-69")
//    def "handle exclude with circular dependency"() {
//        given:
//        repo.module("shadow", "c", "1.0")
//            .insertFile("c.properties", "c")
//            .dependsOn("d")
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
//    }
//
}