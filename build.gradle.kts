/*
 * Copyright (c) 2019-2020 Owain van Brakel <https://github.com/Owain94>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import org.ajoberstar.grgit.Grgit

// Define repository URLs as variables for easy maintenance
def openOsrsHosting = "https://raw.githubusercontent.com/open-osrs/hosting/master"
def jitpackUrl = "https://jitpack.io"
def runeliteRepo = "https://repo.runelite.net"

buildscript {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        maven(url = openOsrsHosting)
    }
    dependencies {
        classpath("org.ajoberstar.grgit:grgit-core:4.1.0")
    }
}

plugins {
    id "org.ajoberstar.grgit" version "4.1.0"
    application
}

val localGitCommit: String = try {
    Grgit.open(mapOf("dir" to rootProject.projectDir.absolutePath)).head().id
} catch (_: Exception) {
    "n/a"
}

allprojects {
    group = "com.openosrs"
    version = ProjectVersions.openosrsVersion
    apply plugin: 'maven-publish'
}

subprojects {
    repositories {
        mavenLocal()
        mavenCentral()
        maven(url = jitpackUrl)
        maven(url = runeliteRepo)
        maven(url = openOsrsHosting)
    }

    apply plugin: 'java-library'

    project.extra["gitCommit"] = localGitCommit
    project.extra["rootPath"] = rootDir.toString().replace("\\", "/")

    if (this.name != "runescape-client") {
        apply plugin: 'checkstyle'
        checkstyle {
            maxWarnings = 0
            toolVersion = "9.1"
            showViolations = true
            ignoreFailures = false
        }
    }

    publishing {
        repositories {
            maven {
                url = uri("$buildDir/repo")
            }
            if (System.getenv("REPO_URL") != null) {
                maven {
                    url = uri(System.getenv("REPO_URL"))
                    credentials {
                        username = System.getenv("REPO_USERNAME")
                        password = System.getenv("REPO_PASSWORD")
                    }
                }
            }
        }
        publications {
            create("mavenJava", MavenPublication::class) {
                from(components["java"])
            }
        }
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    tasks.withType<Jar> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        dirMode = 493
        fileMode = 420
        doLast {
            // Signing configuration for release builds
        }
    }

    configurations["compileOnly"].extendsFrom(configurations["annotationProcessor"])
}

application {
    mainClass.set("net.runelite.client.RuneLite")
}

tasks.named<JavaExec>("run") {
    classpath = project(":runelite-client").sourceSets.main.get().runtimeClasspath
    enableAssertions = true
}