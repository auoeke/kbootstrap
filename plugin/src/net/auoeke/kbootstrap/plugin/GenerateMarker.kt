package net.auoeke.kbootstrap.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import java.io.File
import javax.inject.Inject

@Suppress("LeakingThis")
open class GenerateMarker @Inject constructor(private val set: SourceSet, private val extension: KBootstrapExtension) : DefaultTask() {
    @OutputDirectory
    val output: File = this.project.buildDir.resolve("generated/resources/all").apply {mkdirs()}

    private val jar: Jar = this.project.tasks.getByName("jar") as Jar

    init {
        this.outputs.upToDateWhen {false}

        this.project.tasks.getByName(this.set.classesTaskName).also {
            this.dependsOn(it)
            it.finalizedBy(this)
        }

        this.jar.dependsOn(this)
        this.project.dependencies.add(this.set.runtimeOnlyConfigurationName, this.project.files(this.output))
    }

    @TaskAction
    fun generate() {
        this.set.output.dir(this.output)
        this.jar.from(this.project.files(this.output))

        this.output.resolve("kbootstrap-modules").writeText(this.extension.modules.joinToString(":"))
    }
}
