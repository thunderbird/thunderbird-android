package net.thunderbird.gradle.plugin.featureflag

import net.thunderbird.gradle.plugin.featureflag.schema.SchemaValidator
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
        logger.lifecycle("[feature-flag] Applied on root project: $this")

        val schemaValidator = SchemaValidator(validateFormats = extension.validateFormats.orElse(true).get())
        when (
            val result = schemaValidator.validate(
                schemaFile = extension.schema.asFile.get(),
                catalog = extension.catalog.asFile.get(),
            )
        ) {
            is SchemaValidator.Result.Error.FileNotFound -> throw GradleException(
                "Failed to apply feature flag plugin. Reason: File '${result.path}' not found.",
            )

            is SchemaValidator.Result.Error.ValidationFailed -> {
                val detail = result.errors.joinToString(System.lineSeparator()) { error ->
                    "- $error"
                }
                val message = "Feature flag catalog JSON validation failed for ${result.catalog.path} against ${
                    result.schema.path
                }:${System.lineSeparator()}$detail"

                throw GradleException(message)
            }

            SchemaValidator.Result.Success -> Unit // TODO(#11327): register FF key enum generation task
        }
    }
}
