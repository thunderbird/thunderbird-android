import com.android.SdkConstants
import com.android.build.api.artifact.SingleArtifact
import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator

/**
 * This is a Gradle plugin that adds a task to generate the badging of the APKs and a task to check that the
 * generated badging is the same as the golden badging.
 *
 * This is taken from [nowinandroid](https://github.com/android/nowinandroid) and follows recommendations from
 * [Prevent regressions with CI and badging](https://android-developers.googleblog.com/2023/12/increase-your-apps-availability-across-device-types.html).
 */

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val variantsToCheck = listOf("release", "beta", "daily")

androidComponents {
    onVariants { variant ->
        if (variantsToCheck.any { variant.name.contains(it, ignoreCase = true) }) {
            val capitalizedVariantName = variant.name.capitalized()
            val generateBadgingTaskName = "generate${capitalizedVariantName}Badging"
            val generateBadging = tasks.register<GenerateBadgingTask>(generateBadgingTaskName) {
                apk.set(variant.artifacts.get(SingleArtifact.APK_FROM_BUNDLE))
                aapt2Executable.set(
                    File(
                        android.sdkDirectory,
                        "${SdkConstants.FD_BUILD_TOOLS}/" +
                            "${android.buildToolsVersion}/" +
                            SdkConstants.FN_AAPT2,
                    ),
                )
                badging.set(
                    project.layout.buildDirectory.file(
                        "outputs/apk_from_bundle/${variant.name}/${variant.name}-badging.txt",
                    ),
                )
            }

            val updateBadgingTaskName = "update${capitalizedVariantName}Badging"
            tasks.register<Copy>(updateBadgingTaskName) {
                from(generateBadging.get().badging)
                into(project.layout.projectDirectory.dir("badging"))
            }

            val checkBadgingTaskName = "check${capitalizedVariantName}Badging"
            val goldenBadgingPath = project.layout.projectDirectory.file("badging/${variant.name}-badging.txt")
            tasks.register<CheckBadgingTask>(checkBadgingTaskName) {
                if (goldenBadgingPath.asFile.exists()) {
                    goldenBadging.set(goldenBadgingPath)
                }
                generatedBadging.set(
                    generateBadging.get().badging,
                )
                this.updateBadgingTaskName.set(updateBadgingTaskName)

                output.set(
                    project.layout.buildDirectory.dir("intermediates/$checkBadgingTaskName"),
                )
            }

            tasks.named("build") {
                dependsOn(checkBadgingTaskName)
            }
        }
    }
}

private fun String.capitalized() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

@CacheableTask
abstract class GenerateBadgingTask : DefaultTask() {

    @get:OutputFile
    abstract val badging: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val apk: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:InputFile
    abstract val aapt2Executable: RegularFileProperty

    @get:Inject
    abstract val execOperations: ExecOperations

    @TaskAction
    fun taskAction() {
        execOperations.exec {
            commandLine(
                aapt2Executable.get().asFile.absolutePath,
                "dump",
                "badging",
                apk.get().asFile.absolutePath,
            )
            standardOutput = badging.asFile.get().outputStream()
        }
    }
}

@CacheableTask
abstract class CheckBadgingTask : DefaultTask() {

    // In order for the task to be up-to-date when the inputs have not changed,
    // the task must declare an output, even if it's not used. Tasks with no
    // output are always run regardless of whether the inputs changed
    @get:OutputDirectory
    abstract val output: DirectoryProperty

    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    @get:InputFile
    abstract val goldenBadging: RegularFileProperty

    @get:PathSensitive(PathSensitivity.NONE)
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
