package net.thunderbird.gradle.plugin.app.versioning

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

abstract class PrintVersionInfoTask : DefaultTask() {
    @get:Input
    abstract val applicationId: Property<String>

    @get:Input
    abstract val applicationLabel: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionNameSuffix: Property<String>

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @get:InputFiles
    @get:Optional
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stringsXmlFile: RegularFileProperty

    init {
        outputs.upToDateWhen { false } // This forces Gradle to always re-run the task
    }

    @TaskAction
    fun printVersionInfo() {
        val output = """
            APPLICATION_ID=${applicationId.get()}
            APPLICATION_LABEL=${applicationLabel.get()}
            VERSION_CODE=${versionCode.get()}
            VERSION_NAME=${versionName.get()}
            VERSION_NAME_SUFFIX=${versionNameSuffix.get()}
            FULL_VERSION_NAME=${versionName.get()}${versionNameSuffix.get()}
        """.trimIndent()

        println(output)

        if (outputFile.isPresent) {
            outputFile.get().asFile.writeText(output + "\n")
        }
    }
}
