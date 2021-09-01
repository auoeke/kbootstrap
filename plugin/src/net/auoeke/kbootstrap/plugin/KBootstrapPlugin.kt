package net.auoeke.kbootstrap.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS", "unused")
class KBootstrapPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.run {
            apply("org.jetbrains.kotlin.jvm")
            apply("org.jetbrains.kotlin.plugin.serialization")
        }

        val java = project.extensions.getByType(JavaPluginExtension::class.java)

        project.tasks.withType(KotlinCompile::class.java) {compileKotlin ->
            compileKotlin.kotlinOptions.jvmTarget = java.targetCompatibility.toString()
        }

        val extension = KBootstrapExtension(project)
        project.extensions.add("kbootstrap", extension)

        val task = project.tasks.create("kbootstrapMarker", GenerateMarker::class.java, java.sourceSets.getByName("main"), extension)

        project.afterEvaluate {
            project.dependencies.run {
                include(modApi("net.auoeke:kbootstrap:latest.release"))
                compileOnlyApi("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:latest.release")
                compileOnlyApi("org.jetbrains.kotlinx:kotlinx-serialization-json-jvm:latest.release")
            }

            extension.modules.forEach {
                when (it) {
                    "coroutines" -> project.dependencies.run {
                        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-coroutines-core:latest.release")
                        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:latest.release")
                        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:latest.release")
                        compileOnlyApi("org.jetbrains.kotlinx:kotlinx-coroutines-jdk9:latest.release")
                    }
                    "reflect" -> project.dependencies.compileOnlyApi("org.jetbrains.kotlin:kotlin-reflect:latest.release")
                }
            }

            task.generate()
        }
    }

    private fun DependencyHandler.compileOnlyApi(dependency: Any): Dependency = this.add("compileOnlyApi", dependency)!!
    private fun DependencyHandler.modApi(dependency: Any): Dependency = this.add("modApi", dependency)!!
    private fun DependencyHandler.include(dependency: Any): Dependency = this.add("include", dependency)!!
}
