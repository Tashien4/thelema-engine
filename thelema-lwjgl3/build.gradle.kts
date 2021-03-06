plugins {
    kotlin("jvm")
    `maven-publish`
    id("com.jfrog.bintray")
    id("org.jetbrains.dokka")
}

val thelemaGroup: String by project
group = thelemaGroup

val gitRepositoryUrl: String by project

val thelemaVersion: String by project
val verName = thelemaVersion
version = verName

repositories {
    jcenter()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    api(project(":thelema-jvm"))
    api(project(":thelema-core"))

    testImplementation(project(path = ":thelema-core-tests"))
    testImplementation(project(path = ":thelema-ode4j"))

    val lwjglVersion = "3.2.3"
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))
    implementation("org.lwjgl", "lwjgl")
    implementation("org.lwjgl", "lwjgl-glfw")
    implementation("org.lwjgl", "lwjgl-jemalloc")
    implementation("org.lwjgl", "lwjgl-openal")
    implementation("org.lwjgl", "lwjgl-opengl")
    implementation("org.lwjgl", "lwjgl-stb")

    val platforms = arrayOf("natives-linux", "natives-windows", "natives-windows-x86", "natives-macos")
    platforms.forEach {
        implementation("org.lwjgl", "lwjgl", classifier = it)
        implementation("org.lwjgl", "lwjgl-glfw", classifier = it)
        implementation("org.lwjgl", "lwjgl-jemalloc", classifier = it)
        implementation("org.lwjgl", "lwjgl-openal", classifier = it)
        implementation("org.lwjgl", "lwjgl-opengl", classifier = it)
        implementation("org.lwjgl", "lwjgl-stb", classifier = it)
    }
}

tasks {
    jar {
        from({
            configurations.runtimeClasspath.get().map {
                if (it.isDirectory) {
                    it
                } else {
                    val name = it.name.toLowerCase()
                    if (name.contains("lwjgl") ||
                        name.contains("json") ||
                        name.contains("thelema")) {
                        zipTree(it)
                    } else {
                        null
                    }
                }
            }
        })
    }
}

tasks.create("testsJar", Jar::class) {
    archiveBaseName.set("thelema-lwjgl3-tests")

    manifest {
        attributes["Main-Class"] = "org.ksdfv.thelema.lwjgl3.test.MainLwjgl3Tests"
    }

    from(sourceSets.main.get().output)
    from(sourceSets.test.get().output)

    from({
        configurations.testRuntimeClasspath.get().map {
            if (it.isDirectory) {
                it
            } else {
                if (it.extension.toLowerCase() == "jar") {
                    zipTree(it)
                } else {
                    it
                }
            }
        }
    })
}

val sourcesJar by tasks.registering(Jar::class) {
    dependsOn("classes")
    archiveClassifier.set("sources")
    from(sourceSets.main.get().allSource)
}

val javadocJar by tasks.registering(Jar::class) {
    dependsOn(tasks.getByName("dokkaJavadoc"))
    archiveClassifier.set("javadoc")
    from("$buildDir/javadoc")
}

bintray {
    // user and key properties must be saved in user home (by default ~/.gradle/gradle.properties)
    user = project.property("BINTRAY_USER") as String
    key = project.property("BINTRAY_KEY") as String
    override = true
    publish = true
    setPublications("mavenJava")
    pkg.apply {
        repo = "thelema-engine"
        name = "thelema-lwjgl3"
        setLicenses("Apache-2.0")
        vcsUrl = gitRepositoryUrl

        version.apply {
            name = verName
        }
    }
}

publishing {
    publications {
        register("mavenJava", MavenPublication::class) {
            from(components["java"])
            artifact(sourcesJar.get())
            artifact(javadocJar.get())

            pom {
                name.set("thelema-engine")
                url.set(gitRepositoryUrl)

                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }

                developers {
                    developer {
                        id.set("zeganstyl")
                        name.set("Anton Trushkov")
                        email.set("zeganstyl@gmail.com")
                    }
                }

                scm {
                    url.set(gitRepositoryUrl)
                }
            }
        }
    }
}
