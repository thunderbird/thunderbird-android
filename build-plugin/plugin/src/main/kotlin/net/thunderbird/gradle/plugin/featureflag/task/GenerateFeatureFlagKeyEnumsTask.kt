package net.thunderbird.gradle.plugin.featureflag.task

import javax.inject.Inject
import kotlinx.serialization.json.Json
import net.thunderbird.gradle.plugin.featureflag.FeatureFlagCatalog
import net.thunderbird.gradle.plugin.featureflag.FeatureFlagPluginExtension
import net.thunderbird.gradle.plugin.featureflag.codegen.FeatureFlagKeyWriter
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.register
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Gradle task that generates enum classes containing feature flag keys from a feature flag catalog.
 */
@CacheableTask
internal abstract class GenerateFeatureFlagKeyEnumsTask @Inject constructor(
    private val providerFactory: ProviderFactory,
) : DefaultTask() {

    /**
     * The name of the project for which feature flag key enums are being generated.
     * Used for logging purposes during the generation process.
     */
    @get:Input
    abstract val projectName: Property<String>

    /**
     * Input file containing the feature flag catalog definition in JSON format.
     *
     * The catalog defines available feature flags, their default values, context attributes,
     * and app-specific overrides for different build variants.
     */
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val flagCatalog: RegularFileProperty

    /**
     * The target directory where the generated feature flag key enum files will be written.
     * Defaults to 'build/generated/featureflags/src/commonMain/kotlin' if not specified.
     */
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    /**
     * The package name for the generated feature flag key enums.
     */
    @get:Input
    abstract val featureFlagKeysPackageName: Property<String>

    /**
     * The base name for the generated feature flag key enum class.
     *
     * This name is used by the FeatureFlagKeyWriter to create an enum that implements FeatureFlagKey,
     * containing all flag keys defined in the feature flag catalog.
     */
    @get:Input
    abstract val featureFlagKeyEnumBaseName: Property<String>

    private val writer
        get() = FeatureFlagKeyWriter(
            packageName = featureFlagKeysPackageName.get(),
            enumName = featureFlagKeyEnumBaseName.get(),
        )

    companion object {
        const val TASK_NAME = "generateFeatureFlagKeyEnums"
    }

    init {
        group = "Thunderbird Feature Flags"
        description = "Generate the feature flag keys enums"
    }

    /**
     * Gradle task action that generates feature flag key enums from a catalog file.
     *
     * Reads the feature flag catalog from the configured input file, parses it as JSON,
     * and generates a Kotlin enum class containing all feature flag keys defined in the catalog.
     * The generated enum is written to the configured output directory.
     *
     * @throws kotlinx.serialization.SerializationException if the catalog file cannot be parsed
     * @throws java.io.IOException if the catalog file cannot be read or output cannot be written
     */
    @TaskAction
    internal fun generate() {
        logger.info("[feature-flag] generating feature-flag keys enums for ${projectName.get()}")
        logger.debug("[feature-flag] reading catalog")
        val catalog = readCatalog()
        logger.debug("[feature-flag] writing feature-flag keys enum.")
        val outputDir = outputDirectory.asFile.get()
        writer.write(catalog = catalog, outputDir = outputDir)
        logger.info("[feature-flag] wrote feature-flag keys enum to $outputDir.")
    }

    /**
     * Reads and deserializes the feature flag catalog from the configured catalog file.
     *
     * @return The deserialized FeatureFlagCatalog containing version, context, flags, and overrides
     */
    internal fun readCatalog(): FeatureFlagCatalog {
        val catalogText = providerFactory.fileContents(flagCatalog).asText.orNull ?: throw GradleException(
            "Failed to run $TASK_NAME. Reason: Feature flag catalog not found at '${flagCatalog.asFile.get().path}'.",
        )
        return Json.decodeFromString<FeatureFlagCatalog>(catalogText)
    }
}

/**
 * Registers the [GenerateFeatureFlagKeyEnumsTask] on this project.
 *
 * The task is registered and never resolved, so it is not created at configuration time.
 */
internal fun Project.registerFeatureFlagKeyEnumsTask(
    extension: FeatureFlagPluginExtension,
): TaskProvider<GenerateFeatureFlagKeyEnumsTask> {
    val owningProjectName = name
    val defaultOutputDirectory = layout.buildDirectory.dir(DEFAULT_OUTPUT_DIRECTORY)

    return tasks.register<GenerateFeatureFlagKeyEnumsTask>(GenerateFeatureFlagKeyEnumsTask.TASK_NAME) {
        projectName.set(owningProjectName)
        featureFlagKeysPackageName.set(
            extension
                .featureFlagKeys
                .featureFlagKeysPackageName
                .orElse(DEFAULT_KEYS_PACKAGE_NAME),
        )
        flagCatalog.set(extension.catalog)
        outputDirectory.set(extension.featureFlagKeys.outputDirectory.orElse(defaultOutputDirectory))
        featureFlagKeyEnumBaseName.set(extension.featureFlagKeys.featureFlagKeyEnumBaseName)
    }
}

/**
 * Wires the generated sources into the common source set of a Kotlin Multiplatform project.
 *
 * The [TaskProvider] is mapped instead of resolved, so the resulting provider carries the task
 * dependency: every compilation consuming the source set runs the generation first.
 */
internal fun Project.wireGeneratedSourcesIntoKmpSourceSet(
    taskProvider: TaskProvider<GenerateFeatureFlagKeyEnumsTask>,
) {
    logger.debug("[feature-flag] wiring generated sources into '{}' of {}", COMMON_MAIN_SOURCE_SET, this)
    extensions
        .getByType<KotlinMultiplatformExtension>()
        .sourceSets
        .named(COMMON_MAIN_SOURCE_SET) {
            kotlin.srcDir(taskProvider.flatMap { task -> task.outputDirectory })
        }
}

private const val COMMON_MAIN_SOURCE_SET = "commonMain"
private const val DEFAULT_KEYS_PACKAGE_NAME = "net.thunderbird.core.featureflag.keys"
private const val DEFAULT_OUTPUT_DIRECTORY = "generated/featureflags/src/commonMain/kotlin"
