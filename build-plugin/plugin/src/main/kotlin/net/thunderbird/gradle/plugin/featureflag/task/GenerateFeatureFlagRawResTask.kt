package net.thunderbird.gradle.plugin.featureflag.task

import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import java.util.Locale
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.findByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

/**
 * Copies the feature-flag catalog into a generated Android `res/raw` directory so it can be exposed
 * as an `R.raw.*` resource on the owning Android module.
 *
 * The catalog keeps living under `config/featureflag/` (the single source of truth declared in the
 * root `featureFlag {}` extension). Android `res` filenames may only contain lowercase letters,
 * digits and underscores, so the dotted catalog file name is sanitized via the [resourceName]
 * rename performed here.
 */
@CacheableTask
abstract class GenerateFeatureFlagRawResTask : DefaultTask() {

    /**
     * The feature flag catalog file (JSON) to expose as a raw resource.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val catalog: RegularFileProperty

    /**
     * The resource name (without extension) used for the generated file.
     */
    @get:Input
    abstract val resourceName: Property<String>

    /**
     * The res *root* directory; the `raw/` folder that AGP scans is created inside it.
     */
    @get:OutputDirectory
    abstract val outputResDir: DirectoryProperty

    /**
     * The sanitized resource name (without extension) used for the generated file, e.g.
     * `thunderbird-mobile-featureflag.catalog` produces `R.raw.thunderbird_mobile_featureflag_catalog`.
     */
    private val sanitizedResourceName: String
        get() = resourceName
            .get()
            .lowercase(Locale.getDefault())
            .replace("[^a-z0-9_]".toRegex(), "_")

    init {
        group = "Thunderbird Feature Flags"
        description = "Copies the feature-flag catalog into a generated res/raw directory."
    }

    @TaskAction
    fun generate() {
        val rawDir = outputResDir.get().asFile.resolve("raw")
        rawDir.deleteRecursively()
        rawDir.mkdirs()
        catalog.get().asFile.copyTo(rawDir.resolve("$sanitizedResourceName.json"), overwrite = true)
    }

    companion object {
        const val TASK_NAME = "generateFeatureFlagRawRes"
    }
}

fun TaskContainer.registerGenerateFeatureFlagRawResTask(
    project: Project,
): TaskProvider<GenerateFeatureFlagRawResTask> {
    val extensions = project.extensions

    @Suppress("UnstableApiUsage")
    val featureFlagCatalog = project.isolated.rootProject.projectDirectory
        .file("config/featureflag/thunderbird_mobile_featureflag.catalog.json")
    val task = register<GenerateFeatureFlagRawResTask>(GenerateFeatureFlagRawResTask.TASK_NAME) {
        catalog.set(featureFlagCatalog)
        resourceName.set("thunderbird_mobile_featureflag_catalog")
        outputResDir.set(project.layout.buildDirectory.dir("generated/featureflags/res"))
    }

    // Android consumes the catalog as `R.raw.*`, so the generated res root is wired into the variant's layered
    // resources.
    // Although AS may show that the R.raw.thunderbird_mobile_featureflag_catalog is an unresolved reference
    // the compiler is fine with it. Probably an AGP <-> KMP IDE resolution issue.
    extensions.findByType<KotlinMultiplatformAndroidComponentsExtension>()?.onVariants { variant ->
        variant.sources.res?.addGeneratedSourceDirectory(
            taskProvider = task,
            wiredWith = GenerateFeatureFlagRawResTask::outputResDir,
        )
    }

    val generatedRawDir = task.flatMap { it.outputResDir.dir("raw") }
    extensions.findByType<KotlinMultiplatformExtension>()
        ?.targets
        ?.filter { it.platformType == KotlinPlatformType.jvm }
        ?.flatMap { it.compilations }
        ?.filter { it.name == KotlinCompilation.MAIN_COMPILATION_NAME }
        ?.forEach { compilation ->
            compilation
                .defaultSourceSet
                .resources
                .srcDir(generatedRawDir)
        }

    return task
}
