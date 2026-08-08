package net.thunderbird.gradle.plugin.featureflag.validator

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.prop
import java.io.File
import net.thunderbird.gradle.plugin.featureflag.fake.FakeData
import org.junit.Test

internal class SchemaValidatorTest {

    @Test
    fun `validate should return Success when catalog conforms to schema`() {
        // Arrange
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaContents = FakeData.SCHEMA, catalogContents = FakeData.VALID_CATALOG)

        // Assert
        assertThat(result).isEqualTo(SchemaValidator.Result.Success)
    }

    @Test
    fun `validate should return ValidationFailed with given schema and catalog when required property is missing`() {
        // Arrange
        // language=json
        val catalogContents = """
            {
              "flags": []
            }
        """.trimIndent()
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaContents = FakeData.SCHEMA, catalogContents = catalogContents)

        // Assert
        assertThat(result).isInstanceOf<SchemaValidator.Result.ValidationFailed>().all {
            prop(SchemaValidator.Result.ValidationFailed::schemaContents).isEqualTo(FakeData.SCHEMA)
            prop(SchemaValidator.Result.ValidationFailed::catalogContents).isEqualTo(catalogContents)
            prop(SchemaValidator.Result.ValidationFailed::errors).isNotEmpty()
        }
    }

    @Test
    fun `validate should return ValidationFailed when catalog has an unknown property`() {
        // Arrange
        // language=json
        val catalogContents = """
            {
              "version": "2026-07-30.1",
              "flags": [],
              "unknown_property": true
            }
        """.trimIndent()
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaContents = FakeData.SCHEMA, catalogContents = catalogContents)

        // Assert
        assertThat(result)
            .isInstanceOf<SchemaValidator.Result.ValidationFailed>()
            .prop(SchemaValidator.Result.ValidationFailed::errors)
            .isNotEmpty()
    }

    @Test
    fun `validate should report every schema violation of the catalog`() {
        // Arrange
        // language=json
        val catalogContents = """
            {
              "version": "2026-07-30.1",
              "flags": [
                {
                  "key": "Invalid Key",
                  "default": "not-a-boolean"
                }
              ]
            }
        """.trimIndent()
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaContents = FakeData.SCHEMA, catalogContents = catalogContents)

        // Assert
        assertThat(result)
            .isInstanceOf<SchemaValidator.Result.ValidationFailed>()
            .prop(SchemaValidator.Result.ValidationFailed::errors)
            .transform { errors -> errors.joinToString(separator = "\n") }
            .all {
                contains("pattern")
                contains("boolean")
            }
    }

    @Test
    fun `validate should return ValidationFailed when format is invalid and formats are validated`() {
        // Arrange
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(
            schemaContents = FakeData.SCHEMA,
            catalogContents = FakeData.CATALOG_WITH_INVALID_DATE_FORMAT,
        )

        // Assert
        assertThat(result)
            .isInstanceOf<SchemaValidator.Result.ValidationFailed>()
            .prop(SchemaValidator.Result.ValidationFailed::errors)
            .transform { errors -> errors.joinToString(separator = "\n") }
            .contains("date")
    }

    @Test
    fun `validate should return Success when format is invalid and formats are not validated`() {
        // Arrange
        val testSubject = SchemaValidator(validateFormats = false)

        // Act
        val result = testSubject.validate(
            schemaContents = FakeData.SCHEMA,
            catalogContents = FakeData.CATALOG_WITH_INVALID_DATE_FORMAT,
        )

        // Assert
        assertThat(result).isEqualTo(SchemaValidator.Result.Success)
    }

    @Test
    fun `validate should return Success when override keys match the flag key format`() {
        // Arrange
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(
            schemaContents = catalogSchemaContents(),
            catalogContents = FakeData.catalogWithOverrideKey("archive_marks_as_read"),
        )

        // Assert
        assertThat(result).isEqualTo(SchemaValidator.Result.Success)
    }

    @Test
    fun `validate should return ValidationFailed when an override key does not match the flag key format`() {
        // Arrange
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(
            schemaContents = catalogSchemaContents(),
            catalogContents = FakeData.catalogWithOverrideKey("Foo"),
        )

        // Assert
        assertThat(result)
            .isInstanceOf<SchemaValidator.Result.ValidationFailed>()
            .prop(SchemaValidator.Result.ValidationFailed::errors)
            .isNotEmpty()
    }

    /**
     * The override key format is a rule of the shipped catalog schema, so these cases validate against the real
     * schema instead of a fake one.
     */
    private fun catalogSchemaContents(): String =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .map { root -> File(root, CATALOG_SCHEMA_PATH) }
            .firstOrNull { it.isFile }
            ?.readText(Charsets.UTF_8)
            ?: error("Could not find '$CATALOG_SCHEMA_PATH' in any parent of '${File("").absolutePath}'.")

    private companion object {
        const val CATALOG_SCHEMA_PATH = "config/featureflag/thunderbird_mobile_featureflag.schema.json"
    }
}
