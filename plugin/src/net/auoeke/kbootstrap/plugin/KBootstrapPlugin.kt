package net.auoeke.kbootstrap.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "unused")
class KBootstrapPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.apply("org.jetbrains.kotlin.jvm")

        val java = project.extensions.getByType(JavaPluginExtension::class.java)

        val extension = KBootstrapExtension()
        project.extensions.add("kbootstrap", extension)

        val outputDirectory = project.buildDir.resolve("generated/resources/all").apply {mkdirs()}

        java.sourceSets.all {
            it.output.dir(outputDirectory)
            project.dependencies.add(it.runtimeOnlyConfigurationName, project.files(outputDirectory))
        }
        // val it = java.sourceSets.getByName("main")

        project.afterEvaluate {
            project.dependencies.add("include", project.dependencies.add("modApi", "net.auoeke:kbootstrap:latest.release"))
            project.tasks.withType(KotlinCompile::class.java) {
                it.kotlinOptions.jvmTarget = java.targetCompatibility.toString()
            }

            extension.modules.forEach {
                when (it) {
                    "coroutines" -> with(project.dependencies) {
                        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-coroutines-core:latest.release")
                        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:latest.release")
                        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:latest.release")
                        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-coroutines-jdk9:latest.release")
                    }
                    "reflect" -> project.dependencies.compileOnlyApi("org.jetbrains.kotlin:kotlin-reflect:latest.release")
                    "serialization" -> with(project.dependencies) {
                        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:latest.release")
                        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:latest.release")
                    }
                }

            }

            outputDirectory.resolve("kbootstrap-modules").writeText(extension.modules.joinToString(":"))
        }
    }

    private fun DependencyHandler.compileOnlyApi(dependency: Any) {
        this.add("compileOnlyApi", dependency)
    }
}
