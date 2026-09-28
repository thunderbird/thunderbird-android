package net.thunderbird.gradle.plugin.featureflag

import java.io.File
import kotlinx.serialization.json.Json
import net.thunderbird.gradle.plugin.featureflag.validator.CatalogValidator
import net.thunderbird.gradle.plugin.featureflag.validator.SchemaValidator
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.kotlin.dsl.create

/**
 * Root Gradle plugin for managing and validating feature flag catalogs.
 *
 * The plugin only activates when applied to the root project; applying it to subprojects has no effect.
 * All validation occurs during project evaluation after the extension has been configured.
 *
 * @throws GradleException if the catalog or schema files are missing, not found, or validation fails
 */
@Suppress("unused")
abstract class FeatureFlagRootPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        val extension = target.extensions.create<FeatureFlagPluginExtension>(FeatureFlagPluginExtension.NAME)
        when {
            isRootProject -> afterEvaluate {
                extension.validate()
                applyForRootProject(extension)
            }

            else -> Unit
        }
    }

    private val Project.isRootProject: Boolean
        get() = this == rootProject

    private fun Project.applyForRootProject(extension: FeatureFlagPluginExtension) {
        logger.info("[feature-flag] Applied on root project: $this")

        val schemaValidator = SchemaValidator(validateFormats = extension.validateFormats.orElse(true).get())
        val catalog = extension.catalog.asFile.get()
        val catalogContents = readTextOrNull(extension.catalog) ?: throw GradleException(
            "Failed to apply feature flag plugin. Reason: The feature flag catalog was not found at '${catalog.path}'.",
        )

        val schema = extension.schema.asFile.get()
        val schemaContents = readTextOrNull(extension.schema) ?: throw GradleException(
            "Failed to apply feature flag plugin. Reason: The feature flag schema was not found at '$schema'.",
        )

        validateSchema(extension, schemaValidator, schemaContents, catalogContents, catalog)
        validateCatalog(catalogContents)
    }

    /**
     * Reads [file] through the provider API so that the configuration cache tracks it as an input.
     *
     * Reading a file directly, e.g. with [File.readText], happens outside of Gradle's view: a cached
     * configuration would be reused after editing the catalog or the schema, silently skipping the
     * validation below.
     *
     * @return the file content, or `null` when the file does not exist.
     */
    private fun Project.readTextOrNull(file: RegularFileProperty): String? =
        providers.fileContents(file).asText.orNull

    private fun validateSchema(
        extension: FeatureFlagPluginExtension,
        schemaValidator: SchemaValidator,
        schemaContents: String,
        catalogContents: String,
        catalog: File,
    ): Unit = when (val result = schemaValidator.validate(schemaContents, catalogContents)) {
        is SchemaValidator.Result.ValidationFailed -> {
            val detail = result.errors.joinToString(System.lineSeparator()) { error ->
                "- $error"
            }
            val message = "Feature flag catalog JSON validation failed for ${catalog.path} against ${
                extension.schema.asFile.get().path
            }:${System.lineSeparator()}$detail"

            throw GradleException(message)
        }

        SchemaValidator.Result.Success -> Unit
    }

    private fun validateCatalog(catalogAsText: String) {
        val featureFlagCatalog = Json.decodeFromString<FeatureFlagCatalog>(catalogAsText)
        when (val result = CatalogValidator().validate(featureFlagCatalog)) {
            is CatalogValidator.Result.Error.ValidationError -> throw GradleException(
                "Failed to validate Feature flag catalog. Reason: \n${result.message}",
            )

            CatalogValidator.Result.Success -> Unit
        }
    }
}
