package net.thunderbird.gradle.plugin.app.badging

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.io.writeText
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations

@CacheableTask
abstract class GenerateBadgingTask : DefaultTask()  {
    @get:OutputFile
    abstract val badging: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val apk: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val aapt2Executable: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun taskAction() {
        val outputStream = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(
                aapt2Executable.get().asFile.absolutePath,
                "dump",
                "badging",
                apk.get().asFile.absolutePath,
            )
            standardOutput = outputStream
        }

        badging.asFile.get().writeText(cleanBadgingContent(outputStream) + "\n")
    }

    private fun cleanBadgingContent(outputStream: ByteArrayOutputStream): String {
        return ByteArrayInputStream(outputStream.toByteArray()).bufferedReader().use { reader ->
            reader.lineSequence().map { line ->
                line.cleanBadgingLine()
            }.sorted().joinToString("\n")
        }
    }

    private fun String.cleanBadgingLine(): String {
        return if (startsWith("package:")) {
            replace(Regex("versionName='[^']*'"), "")
                .replace(Regex("versionCode='[^']*'"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        } else if (trim().startsWith("uses-feature-not-required:")) {
            trim()
        } else {
            this
        }
    }
}
