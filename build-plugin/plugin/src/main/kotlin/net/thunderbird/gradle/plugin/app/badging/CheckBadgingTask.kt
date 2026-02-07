package net.thunderbird.gradle.plugin.app.badging

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin

@CacheableTask
abstract class CheckBadgingTask : DefaultTask() {

    // In order for the task to be up-to-date when the inputs have not changed,
    // the task must declare an output, even if it's not used. Tasks with no
    // output are always run regardless of whether the inputs changed
    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:Optional
    @get:InputFile
    abstract val goldenBadging: RegularFileProperty

    @get:PathSensitive(PathSensitivity.RELATIVE)
    @get:InputFile
    abstract val generatedBadging: RegularFileProperty

    @get:Input
    abstract val updateBadgingTaskName: Property<String>

    override fun getGroup(): String = LifecycleBasePlugin.VERIFICATION_GROUP

    @TaskAction
    fun taskAction() {
        if (goldenBadging.isPresent.not()) {
            printlnColor(
                ANSI_YELLOW,
                "Golden badging file does not exist!" +
                    " If this is the first time running this task," +
                    " run ./gradlew ${updateBadgingTaskName.get()}",
            )
            return
        }

        val goldenBadgingContent = goldenBadging.get().asFile.readText()
        val generatedBadgingContent = generatedBadging.get().asFile.readText()
        if (goldenBadgingContent == generatedBadgingContent) {
            printlnColor(ANSI_YELLOW, "Generated badging is the same as golden badging!")
            return
        }

        val diff = performDiff(goldenBadgingContent, generatedBadgingContent)
        printDiff(diff)

        throw GradleException(
            """
            Generated badging is different from golden badging!

            If this change is intended, run ./gradlew ${updateBadgingTaskName.get()}
            """.trimIndent(),
        )
    }

    private fun performDiff(goldenBadgingContent: String, generatedBadgingContent: String): String {
        val generator: DiffRowGenerator = DiffRowGenerator.create()
            .showInlineDiffs(true)
            .mergeOriginalRevised(true)
            .inlineDiffByWord(true)
            .oldTag { _ -> "" }
            .newTag { _ -> "" }
            .build()

        return generator.generateDiffRows(
            goldenBadgingContent.lines(),
            generatedBadgingContent.lines(),
        ).filter { row -> row.tag != DiffRow.Tag.EQUAL }
            .joinToString("\n") { row ->
                @Suppress("WHEN_ENUM_CAN_BE_NULL_IN_JAVA")
                when (row.tag) {
                    DiffRow.Tag.INSERT -> {
                        "+ ${row.newLine}"
                    }

                    DiffRow.Tag.DELETE -> {
                        "- ${row.oldLine}"
                    }

                    DiffRow.Tag.CHANGE -> {
                        "+ ${row.newLine}"
                        "- ${row.oldLine}"
                    }

                    DiffRow.Tag.EQUAL -> ""
                }
            }
    }

    private fun printDiff(diff: String) {
        printlnColor("", null)
        printlnColor(ANSI_YELLOW, "Badging diff:")

        diff.lines().forEach { line ->
            val ansiColor = if (line.startsWith("+")) {
                ANSI_GREEN
            } else if (line.startsWith("-")) {
                ANSI_RED
            } else {
                null
            }
            printlnColor(line, ansiColor)
        }
    }

    private fun printlnColor(text: String, ansiColor: String?) {
        println(
            if (ansiColor != null) {
                ansiColor + text + ANSI_RESET
            } else {
                text
            },
        )
    }

    private companion object {
        const val ANSI_RESET = "\u001B[0m"
        const val ANSI_RED = "\u001B[31m"
        const val ANSI_GREEN = "\u001B[32m"
        const val ANSI_YELLOW = "\u001B[33m"
    }
}
