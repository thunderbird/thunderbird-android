package net.thunderbird.gradle.plugin.featureflag.validator

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaRegistry

/**
 * Validates JSON catalog files against JSON schema definitions.
 *
 * This validator loads JSON schema files and verifies that catalog files conform to the schema rules.
 * It supports optional format assertion validation based on configuration.
 *
 * @param validateFormats Whether to enable format assertions during validation
 */
class SchemaValidator(
    private val validateFormats: Boolean,
) {
    /**
     * Validates a JSON catalog file against a JSON schema file.
     *
     * @param schemaContents The JSON schema file contents to validate against
     * @param catalogContents The JSON catalog file contents as text to validate
     * @return Result.Success if validation passes, Result.Error.FileNotFound if either file doesn't exist,
     * or Result.Error.ValidationFailed if the catalog doesn't conform to the schema
     */
    fun validate(schemaContents: String, catalogContents: String): Result {
        val registry = SchemaRegistry
            .builder()
            .schemas { schemaContents }
            .build()

        val schema = registry.getSchema(schemaContents)

        schema.initializeValidators()
        val error = schema.validate(catalogContents, InputFormat.JSON) { context ->
            context.executionConfig { config ->
                config.formatAssertionsEnabled(validateFormats)
            }
        }

        return if (error.isEmpty()) {
            Result.Success
        } else {
            Result.ValidationFailed(schemaContents, catalogContents, errors = error.map { it.message })
        }
    }

    sealed interface Result {
        data object Success : Result
        data class ValidationFailed(
            val schemaContents: String,
            val catalogContents: String,
            val errors: List<String>,
        ) : Result
    }
}
