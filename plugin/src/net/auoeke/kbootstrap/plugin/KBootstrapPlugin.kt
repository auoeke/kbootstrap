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

        project.tasks.withType(KotlinCompile::class.java) {compileKotlin ->
            compileKotlin.kotlinOptions.jvmTarget = java.targetCompatibility.toString()
        }

        val extension = KBootstrapExtension()
        project.extensions.add("kbootstrap", extension)

        val task = project.tasks.create("kbootstrapMarker", GenerateMarker::class.java, java.sourceSets.getByName("main"), extension)

        project.afterEvaluate {
            project.dependencies.add("include", project.dependencies.add("modApi", "net.auoeke:kbootstrap:latest.release"))

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

            task.generate()
        }
    }

    private fun DependencyHandler.compileOnlyApi(dependency: Any) {
        this.add("compileOnlyApi", dependency)
    }
}
