package net.thunderbird.gradle.plugin.featureflag.codegen

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import java.io.File
import net.thunderbird.gradle.plugin.featureflag.FeatureFlagCatalog
import net.thunderbird.gradle.plugin.featureflag.FlagRegistry
import net.thunderbird.gradle.plugin.featureflag.FlagRegistryOverrides
import net.thunderbird.gradle.plugin.featureflag.K9Overrides
import net.thunderbird.gradle.plugin.featureflag.ThunderbirdOverrides
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

internal class FeatureFlagKeyWriterTest {

    @get:Rule
    val outputFolder = TemporaryFolder()

    @Test
    fun `write should create the file named after the enum in the package directory`() {
        // Arrange
        val outputDir = outputFolder.root
        val testSubject = FeatureFlagKeyWriter(packageName = PACKAGE_NAME, enumName = ENUM_NAME)

        // Act
        testSubject.write(catalog = catalog(flags("archive_marks_as_read")), outputDir = outputDir)

        // Assert
        assertThat(outputDir.generatedFile()).exists()
    }

    @Test
    fun `write should generate an enum constant for every flag of the catalog`() {
        // Arrange
        val outputDir = outputFolder.root
        val catalog = catalog(
            listOf(
                FlagRegistry(key = "archive_marks_as_read", default = true, description = "Marks as read"),
                FlagRegistry(key = "email_notifications", default = false, description = "Notifies on new email"),
            ),
        )
        val testSubject = FeatureFlagKeyWriter(packageName = PACKAGE_NAME, enumName = ENUM_NAME)

        // Act
        testSubject.write(catalog = catalog, outputDir = outputDir)

        // Assert
        assertThat(outputDir.generatedFile().readText()).isEqualTo(
            generatedFileContent(
                constants = listOf(
                    """  ARCHIVE_MARKS_AS_READ("archive_marks_as_read", "Marks as read"),""",
                    """  EMAIL_NOTIFICATIONS("email_notifications", "Notifies on new email"),""",
                ).joinToString(separator = "\n"),
            ),
        )
    }

    @Test
    fun `write should generate a null description when the flag declares none`() {
        // Arrange
        val outputDir = outputFolder.root
        val catalog = catalog(flags("archive_marks_as_read"))
        val testSubject = FeatureFlagKeyWriter(packageName = PACKAGE_NAME, enumName = ENUM_NAME)

        // Act
        testSubject.write(catalog = catalog, outputDir = outputDir)

        // Assert
        assertThat(outputDir.generatedFile().readText()).isEqualTo(
            generatedFileContent(constants = """  ARCHIVE_MARKS_AS_READ("archive_marks_as_read", null),"""),
        )
    }

    @Test
    fun `write should generate an empty description when the flag declares a blank one`() {
        // Arrange
        val outputDir = outputFolder.root
        val catalog = catalog(listOf(FlagRegistry(key = "archive_marks_as_read", default = false, description = "")))
        val testSubject = FeatureFlagKeyWriter(packageName = PACKAGE_NAME, enumName = ENUM_NAME)

        // Act
        testSubject.write(catalog = catalog, outputDir = outputDir)

        // Assert
        assertThat(outputDir.generatedFile().readText()).isEqualTo(
            generatedFileContent(constants = """  ARCHIVE_MARKS_AS_READ("archive_marks_as_read", ""),"""),
        )
    }

    @Test
    fun `write should escape quotes, backslashes and dollar signs of the description`() {
        // Arrange
        val outputDir = outputFolder.root
        val catalog = catalog(
            listOf(
                FlagRegistry(
                    key = "archive_marks_as_read",
                    default = false,
                    description = """He said "hi" to ${'$'}user \ backslash""",
                ),
            ),
        )
        val testSubject = FeatureFlagKeyWriter(packageName = PACKAGE_NAME, enumName = ENUM_NAME)

        // Act
        testSubject.write(catalog = catalog, outputDir = outputDir)

        // Assert
        assertThat(outputDir.generatedFile().readText()).isEqualTo(
            generatedFileContent(
                constants = """  ARCHIVE_MARKS_AS_READ("archive_marks_as_read",""" +
                    """ "He said \"hi\" to ${'$'}{'${'$'}'}user \\ backslash"),""",
            ),
        )
    }

    @Test
    fun `write should escape line breaks of the description`() {
        // Arrange
        val outputDir = outputFolder.root
        val catalog = catalog(
            listOf(FlagRegistry(key = "archive_marks_as_read", default = false, description = "first\nsecond")),
        )
        val testSubject = FeatureFlagKeyWriter(packageName = PACKAGE_NAME, enumName = ENUM_NAME)

        // Act
        testSubject.write(catalog = catalog, outputDir = outputDir)

        // Assert
        assertThat(outputDir.generatedFile().readText()).isEqualTo(
            generatedFileContent(
                constants = "  ARCHIVE_MARKS_AS_READ(\"archive_marks_as_read\", \"\"\"\n" +
                    "  |first\n" +
                    "  |second\n" +
                    "  \"\"\".trimMargin()),",
            ),
        )
    }

    @Test
    fun `write should keep the declaration order of the catalog flags`() {
        // Arrange
        val outputDir = outputFolder.root
        val catalog = catalog(flags("zeta_flag", "alpha_flag", "middle_flag"))
        val testSubject = FeatureFlagKeyWriter(packageName = PACKAGE_NAME, enumName = ENUM_NAME)

        // Act
        testSubject.write(catalog = catalog, outputDir = outputDir)

        // Assert
        assertThat(outputDir.generatedFile().enumConstantNames()).isEqualTo(
            listOf("ZETA_FLAG", "ALPHA_FLAG", "MIDDLE_FLAG"),
        )
    }

    @Test
    fun `write should generate an enum without constants when the catalog declares no flag`() {
        // Arrange
        val outputDir = outputFolder.root
        val testSubject = FeatureFlagKeyWriter(packageName = PACKAGE_NAME, enumName = ENUM_NAME)

        // Act
        testSubject.write(catalog = catalog(emptyList()), outputDir = outputDir)

        // Assert
        assertThat(outputDir.generatedFile().enumConstantNames()).isEqualTo(emptyList())
    }

    @Test
    fun `write should generate the file into the directory matching the configured package`() {
        // Arrange
        val outputDir = outputFolder.root
        val packageName = "net.thunderbird.custom.keys"
        val testSubject = FeatureFlagKeyWriter(packageName = packageName, enumName = "CustomKeys")

        // Act
        testSubject.write(catalog = catalog(flags("archive_marks_as_read")), outputDir = outputDir)

        // Assert
        assertThat(
            outputDir.resolve("net/thunderbird/custom/keys/CustomKeys.kt").exists(),
        ).isTrue()
    }

    @Test
    fun `write should overwrite a previously generated file`() {
        // Arrange
        val outputDir = outputFolder.root
        val testSubject = FeatureFlagKeyWriter(packageName = PACKAGE_NAME, enumName = ENUM_NAME)
        testSubject.write(catalog = catalog(flags("archive_marks_as_read")), outputDir = outputDir)

        // Act
        testSubject.write(catalog = catalog(flags("email_notifications")), outputDir = outputDir)

        // Assert
        assertThat(outputDir.generatedFile().enumConstantNames()).isEqualTo(listOf("EMAIL_NOTIFICATIONS"))
    }

    private fun File.generatedFile(): File = resolve("${PACKAGE_NAME.replace('.', '/')}/$ENUM_NAME.kt")

    private fun File.enumConstantNames(): List<String> =
        readLines().mapNotNull { line -> ENUM_CONSTANT_REGEX.find(line)?.groupValues?.get(1) }

    /**
     * Builds the file the writer is expected to generate, with [constants] as its enum constants.
     *
     * The lines are joined instead of written as a single raw string, because generated descriptions
     * spanning multiple lines carry `|` margins that `trimMargin` would strip from the expectation.
     */
    private fun generatedFileContent(constants: String): String = listOf(
        "// !! GENERATED FILE - DO NOT CHANGE! CHANGES ARE GOING TO BE OVERWRITTEN. !!",
        "package net.thunderbird.core.featureflag.keys",
        "",
        "import kotlin.String",
        "import net.thunderbird.core.featureflag.FeatureFlagKey",
        "",
        "public enum class FeatureFlagKeys(",
        "  override val key: String,",
        "  override val description: String?,",
        ") : FeatureFlagKey {",
        constants,
        "  ;",
        "}",
        "",
    ).joinToString(separator = "\n")

    private fun flags(vararg keys: String): List<FlagRegistry> =
        keys.map { key -> FlagRegistry(key = key, default = false) }

    private fun catalog(flags: List<FlagRegistry>): FeatureFlagCatalog = FeatureFlagCatalog(
        version = "2026-07-30.1",
        flags = flags,
        overrides = FlagRegistryOverrides(
            thunderbird = ThunderbirdOverrides(
                debug = emptyMap(),
                daily = emptyMap(),
                beta = emptyMap(),
                release = emptyMap(),
            ),
            k9 = K9Overrides(debug = emptyMap(), release = emptyMap()),
        ),
    )

    private companion object {
        const val PACKAGE_NAME = "net.thunderbird.core.featureflag.keys"
        const val ENUM_NAME = "FeatureFlagKeys"
        val ENUM_CONSTANT_REGEX = """^\s{2}([A-Z0-9_]+)\(""".toRegex()
    }
}
