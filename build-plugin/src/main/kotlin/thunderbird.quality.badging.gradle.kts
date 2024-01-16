import assertk.assertThat
import assertk.assertions.isEqualTo
import com.android.SdkConstants
import com.android.build.api.artifact.SingleArtifact
import org.gradle.configurationcache.extensions.capitalized

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

androidComponents {
    onVariants { variant ->
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
    }
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
            logger.error(
                "Golden badging file does not exist! " +
                    "If this is the first time running this task, " +
                    "run ./gradlew ${updateBadgingTaskName.get()}",
            )
            return
        }

        val goldenBadgingContent = goldenBadging.get().asFile.readText()
        val generatedBadgingContent = generatedBadging.get().asFile.readText()
        if (goldenBadgingContent == generatedBadgingContent) {
            logger.info("Generated badging is the same as golden badging!")
            return
        }

        assertThat(generatedBadgingContent).isEqualTo(goldenBadgingContent)
    }
}


