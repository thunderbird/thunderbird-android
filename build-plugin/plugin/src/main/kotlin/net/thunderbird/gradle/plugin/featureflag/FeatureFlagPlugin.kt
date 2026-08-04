package net.thunderbird.gradle.plugin.featureflag

import java.io.File
import kotlinx.serialization.json.Json
import net.thunderbird.gradle.plugin.featureflag.validator.CatalogValidator
import net.thunderbird.gradle.plugin.featureflag.validator.SchemaValidator
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

/**
 * Gradle plugin that validates feature flag catalog files against JSON schema definitions.
 *
 * This plugin must be applied to the root project. It creates a "featureFlag" extension
 * for configuring catalog and schema files, then validates the catalog against the schema
 * during project evaluation.
 */
@Suppress("unused")
class FeatureFlagPlugin : Plugin<Project> {
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
        if (!catalog.exists()) {
            throw GradleException(
                "Failed to apply feature flag plugin. Reason: The catalog file '${catalog.path}' not found.",
            )
        }

        val catalogAsText = catalog.readText(Charsets.UTF_8)

        val validSchema = validateSchema(schemaValidator, extension, catalogAsText, catalog)
        val validCatalog = validateCatalog(catalogAsText)
        if (validSchema && validCatalog) {
            // TODO(#11327): register Feature Flag key enum generation task
            logger.debug("Registering Feature Flag Key enum generation task.")
        }
    }

    private fun validateSchema(
        schemaValidator: SchemaValidator,
        extension: FeatureFlagPluginExtension,
        catalogAsText: String,
        catalog: File,
    ): Boolean = when (
        val result = schemaValidator.validate(
            schemaFile = extension.schema.asFile.get(),
            catalogAsText = catalogAsText,
        )
    ) {
        is SchemaValidator.Result.Error.FileNotFound -> throw GradleException(
            "Failed to apply feature flag plugin. Reason: File '${result.path}' not found.",
        )

        is SchemaValidator.Result.Error.ValidationFailed -> {
            val detail = result.errors.joinToString(System.lineSeparator()) { error ->
                "- $error"
            }
            val message = "Feature flag catalog JSON validation failed for ${catalog.path} against ${
                result.schema.path
            }:${System.lineSeparator()}$detail"

            throw GradleException(message)
        }

        SchemaValidator.Result.Success -> true
    }

    private fun validateCatalog(catalogAsText: String): Boolean {
        val featureFlagCatalog = Json.decodeFromString<FeatureFlagCatalog>(catalogAsText)
        return when (val result = CatalogValidator().validate(featureFlagCatalog)) {
            is CatalogValidator.Result.Error.ValidationError -> throw GradleException(
                "Failed to validate Feature flag catalog. Reason: \n${result.message}",
            )

            CatalogValidator.Result.Success -> true
        }
    }
}
