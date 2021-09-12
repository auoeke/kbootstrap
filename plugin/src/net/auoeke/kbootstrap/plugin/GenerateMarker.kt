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
    val output: File = project.buildDir.resolve("generated/resources/all").apply {mkdirs()}

    private val jar: Jar = project.tasks.getByName("jar") as Jar

    init {
        this.outputs.upToDateWhen {false}

        project.tasks.getByName(set.classesTaskName).also {
            dependsOn(it)
            it.finalizedBy(this)
        }

        jar.dependsOn(this)
        project.dependencies.add(set.runtimeOnlyConfigurationName, project.files(output))
    }

    @TaskAction
    fun generate() {
        set.output.dir(output)
        jar.from(project.files(output))

        output.resolve("kbootstrap-modules").writeText(this.extension.modules.joinToString(":"))
    }
}
