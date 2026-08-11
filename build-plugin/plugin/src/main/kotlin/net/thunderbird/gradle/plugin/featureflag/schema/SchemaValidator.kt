package net.thunderbird.gradle.plugin.featureflag.schema

import com.networknt.schema.InputFormat
import com.networknt.schema.SchemaRegistry
import java.io.File

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
     * Checks if both files exist, loads the schema, and validates the catalog content against it.
     * Format assertions are enabled or disabled based on the validator configuration.
     *
     * @param schemaFile The JSON schema file to validate against
     * @param catalog The JSON catalog file to validate
     * @return Result.Success if validation passes, Result.Error.FileNotFound if either file doesn't exist,
     * or Result.Error.ValidationFailed if the catalog doesn't conform to the schema
     */
    fun validate(schemaFile: File, catalog: File): Result {
        if (!schemaFile.exists()) return Result.Error.FileNotFound(schemaFile)
        if (!catalog.exists()) return Result.Error.FileNotFound(catalog)

        val registry = SchemaRegistry
            .builder()
            .schemas { schemaFile.readText(Charsets.UTF_8) }
            .build()

        val schema = schemaFile.inputStream().use { registry.getSchema(it) }

        schema.initializeValidators()
        val jsonText = catalog.readText(Charsets.UTF_8)
        val error = schema.validate(jsonText, InputFormat.JSON) { context ->
            context.executionConfig { config ->
                config.formatAssertionsEnabled(validateFormats)
            }
        }

        return if (error.isEmpty()) {
            Result.Success
        } else {
            Result.Error.ValidationFailed(schemaFile, catalog, errors = error.map { it.message })
        }
    }

    sealed interface Result {
        data object Success : Result
        sealed interface Error : Result {
            data class FileNotFound(val path: File) : Error
            data class ValidationFailed(
                val schema: File,
                val catalog: File,
                val errors: List<String>,
            ) : Error
        }
    }
}
