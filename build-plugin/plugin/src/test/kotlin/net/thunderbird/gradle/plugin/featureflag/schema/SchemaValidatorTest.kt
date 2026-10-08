package net.thunderbird.gradle.plugin.featureflag.schema

import assertk.all
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNotEmpty
import assertk.assertions.prop
import java.io.File
import net.thunderbird.gradle.plugin.featureflag.fake.FakeData.CATALOG_WITH_INVALID_DATE_FORMAT
import net.thunderbird.gradle.plugin.featureflag.fake.FakeData.SCHEMA
import net.thunderbird.gradle.plugin.featureflag.fake.FakeData.VALID_CATALOG
import net.thunderbird.gradle.plugin.featureflag.fake.FakeData.catalogWithOverrideKey
import net.thunderbird.gradle.plugin.testing.rule.ProjectTempFolderRule
import org.junit.Rule
import org.junit.Test

internal class SchemaValidatorTest {

    @get:Rule
    val temporaryFolder = ProjectTempFolderRule()

    @Test
    fun `validate should return FileNotFound with schema file when schema does not exist`() {
        // Arrange
        val schemaFile = File(temporaryFolder.root, "missing.schema.json")
        val catalog = writeFile(name = "catalog.json", content = VALID_CATALOG)
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaFile = schemaFile, catalog = catalog)

        // Assert
        assertThat(result)
            .isInstanceOf<SchemaValidator.Result.Error.FileNotFound>()
            .prop(SchemaValidator.Result.Error.FileNotFound::path)
            .isEqualTo(schemaFile)
    }

    @Test
    fun `validate should return FileNotFound with catalog file when catalog does not exist`() {
        // Arrange
        val schemaFile = writeFile(name = "schema.json", content = SCHEMA)
        val catalog = File(temporaryFolder.root, "missing.catalog.json")
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaFile = schemaFile, catalog = catalog)

        // Assert
        assertThat(result)
            .isInstanceOf<SchemaValidator.Result.Error.FileNotFound>()
            .prop(SchemaValidator.Result.Error.FileNotFound::path)
            .isEqualTo(catalog)
    }

    @Test
    fun `validate should return Success when catalog conforms to schema`() {
        // Arrange
        val schemaFile = writeFile(name = "schema.json", content = SCHEMA)
        val catalog = writeFile(name = "catalog.json", content = VALID_CATALOG)
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaFile = schemaFile, catalog = catalog)

        // Assert
        assertThat(result).isEqualTo(SchemaValidator.Result.Success)
    }

    @Test
    fun `validate should return ValidationFailed with given files when required property is missing`() {
        // Arrange
        val schemaFile = writeFile(name = "schema.json", content = SCHEMA)
        val catalog = writeFile(
            name = "catalog.json",
            // language=json
            content = """
                {
                  "flags": []
                }
            """.trimIndent(),
        )
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaFile = schemaFile, catalog = catalog)

        // Assert
        assertThat(result).isInstanceOf<SchemaValidator.Result.Error.ValidationFailed>().all {
            prop(SchemaValidator.Result.Error.ValidationFailed::schema).isEqualTo(schemaFile)
            prop(SchemaValidator.Result.Error.ValidationFailed::catalog).isEqualTo(catalog)
            prop(SchemaValidator.Result.Error.ValidationFailed::errors).isNotEmpty()
        }
    }

    @Test
    fun `validate should return ValidationFailed when catalog has an unknown property`() {
        // Arrange
        val schemaFile = writeFile(name = "schema.json", content = SCHEMA)
        val catalog = writeFile(
            name = "catalog.json",
            content = """
                {
                  "version": "2026-07-30.1",
                  "flags": [],
                  "unknown_property": true
                }
            """.trimIndent(),
        )
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaFile = schemaFile, catalog = catalog)

        // Assert
        assertThat(result)
            .isInstanceOf<SchemaValidator.Result.Error.ValidationFailed>()
            .prop(SchemaValidator.Result.Error.ValidationFailed::errors)
            .isNotEmpty()
    }

    @Test
    fun `validate should report every schema violation of the catalog`() {
        // Arrange
        val schemaFile = writeFile(name = "schema.json", content = SCHEMA)
        val catalog = writeFile(
            name = "catalog.json",
            // language=json
            content = """
                {
                  "version": "2026-07-30.1",
                  "flags": [
                    {
                      "key": "Invalid Key",
                      "default": "not-a-boolean"
                    }
                  ]
                }
            """.trimIndent(),
        )
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaFile = schemaFile, catalog = catalog)

        // Assert
        assertThat(result)
            .isInstanceOf<SchemaValidator.Result.Error.ValidationFailed>()
            .prop(SchemaValidator.Result.Error.ValidationFailed::errors)
            .transform { errors -> errors.joinToString(separator = "\n") }
            .all {
                contains("pattern")
                contains("boolean")
            }
    }

    @Test
    fun `validate should return ValidationFailed when format is invalid and formats are validated`() {
        // Arrange
        val schemaFile = writeFile(name = "schema.json", content = SCHEMA)
        val catalog = writeFile(name = "catalog.json", content = CATALOG_WITH_INVALID_DATE_FORMAT)
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaFile = schemaFile, catalog = catalog)

        // Assert
        assertThat(result)
            .isInstanceOf<SchemaValidator.Result.Error.ValidationFailed>()
            .prop(SchemaValidator.Result.Error.ValidationFailed::errors)
            .transform { errors -> errors.joinToString(separator = "\n") }
            .contains("date")
    }

    @Test
    fun `validate should return Success when format is invalid and formats are not validated`() {
        // Arrange
        val schemaFile = writeFile(name = "schema.json", content = SCHEMA)
        val catalog = writeFile(name = "catalog.json", content = CATALOG_WITH_INVALID_DATE_FORMAT)
        val testSubject = SchemaValidator(validateFormats = false)

        // Act
        val result = testSubject.validate(schemaFile = schemaFile, catalog = catalog)

        // Assert
        assertThat(result).isEqualTo(SchemaValidator.Result.Success)
    }

    @Test
    fun `validate should return Success when override keys match the flag key format`() {
        // Arrange
        val schemaFile = catalogSchemaFile()
        val catalog = writeFile(name = "catalog.json", content = catalogWithOverrideKey("archive_marks_as_read"))
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaFile = schemaFile, catalog = catalog)

        // Assert
        assertThat(result).isEqualTo(SchemaValidator.Result.Success)
    }

    @Test
    fun `validate should return ValidationFailed when an override key does not match the flag key format`() {
        // Arrange
        val schemaFile = catalogSchemaFile()
        val catalog = writeFile(name = "catalog.json", content = catalogWithOverrideKey("Foo"))
        val testSubject = SchemaValidator(validateFormats = true)

        // Act
        val result = testSubject.validate(schemaFile = schemaFile, catalog = catalog)

        // Assert
        assertThat(result)
            .isInstanceOf<SchemaValidator.Result.Error.ValidationFailed>()
            .prop(SchemaValidator.Result.Error.ValidationFailed::errors)
            .isNotEmpty()
    }

    private fun writeFile(name: String, content: String): File =
        temporaryFolder.writeFile(name, content)

    /**
     * The override key format is a rule of the shipped catalog schema, so these cases validate against the real
     * schema instead of a fake one.
     */
    private fun catalogSchemaFile(): File =
        generateSequence(File("").absoluteFile) { it.parentFile }
            .map { root -> File(root, CATALOG_SCHEMA_PATH) }
            .firstOrNull { it.isFile }
            ?: error("Could not find '$CATALOG_SCHEMA_PATH' in any parent of '${File("").absolutePath}'.")

    private companion object {
        const val CATALOG_SCHEMA_PATH = "config/featureflag/thunderbird_mobile_featureflag.schema.json"
    }
}
