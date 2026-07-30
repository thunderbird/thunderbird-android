package net.thunderbird.gradle.plugin.featureflag

import assertk.Assert
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isNotNull
import assertk.assertions.isSuccess
import java.io.File
import net.thunderbird.gradle.plugin.featureflag.fake.FakeData.CATALOG_WITH_INVALID_DATE_FORMAT
import net.thunderbird.gradle.plugin.featureflag.fake.FakeData.INVALID_CATALOG
import net.thunderbird.gradle.plugin.featureflag.fake.FakeData.SCHEMA
import net.thunderbird.gradle.plugin.featureflag.fake.FakeData.VALID_CATALOG
import net.thunderbird.gradle.plugin.testing.rule.ProjectTempFolderRule
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.kotlin.dsl.findByType
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test

internal class FeatureFlagPluginTest {

    @get:Rule
    val temporaryFolder = ProjectTempFolderRule()

    @Test
    fun `apply should register the featureFlag extension`() {
        // Arrange
        val testSubject = rootProject()

        // Act
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)

        // Assert
        assertThat(testSubject.extensions.findByType<FeatureFlagPluginExtension>()).isNotNull()
    }

    @Test
    fun `apply should register the featureFlag extension under the expected name`() {
        // Arrange
        val testSubject = rootProject()

        // Act
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)

        // Assert
        assertThat(testSubject.extensions.findByName(FeatureFlagPluginExtension.NAME)).isNotNull()
    }

    @Test
    fun `evaluating root project should fail when the catalog is not configured`() {
        // Arrange
        val testSubject = rootProject()
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)
        testSubject.featureFlag.schema.set(writeFile(name = "schema.json", content = SCHEMA))

        // Act & Assert
        assertFailure { testSubject.evaluate() }
            .failureMessages()
            .contains("Missing Feature flag catalog file")
    }

    @Test
    fun `evaluating root project should fail when the schema is not configured`() {
        // Arrange
        val testSubject = rootProject()
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)
        testSubject.featureFlag.catalog.set(writeFile(name = "catalog.json", content = VALID_CATALOG))

        // Act & Assert
        assertFailure { testSubject.evaluate() }
            .failureMessages()
            .contains("Missing Feature flag schema file")
    }

    @Test
    fun `evaluating root project should fail when the schema file does not exist`() {
        // Arrange
        val missingSchema = File(temporaryFolder.root, "missing.schema.json")
        val testSubject = rootProject()
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)
        testSubject.featureFlag.schema.set(missingSchema)
        testSubject.featureFlag.catalog.set(writeFile(name = "catalog.json", content = VALID_CATALOG))

        // Act & Assert
        assertFailure { testSubject.evaluate() }
            .failureMessages()
            .contains("Failed to apply feature flag plugin. Reason: File '$missingSchema' not found.")
    }

    @Test
    fun `evaluating root project should fail when the catalog file does not exist`() {
        // Arrange
        val missingCatalog = File(temporaryFolder.root, "missing.catalog.json")
        val testSubject = rootProject()
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)
        testSubject.featureFlag.schema.set(writeFile(name = "schema.json", content = SCHEMA))
        testSubject.featureFlag.catalog.set(missingCatalog)

        // Act & Assert
        assertFailure { testSubject.evaluate() }
            .failureMessages()
            .contains("Failed to apply feature flag plugin. Reason: File '$missingCatalog' not found.")
    }

    @Test
    fun `evaluating root project should fail with the validation details when the catalog is invalid`() {
        // Arrange
        val schema = writeFile(name = "schema.json", content = SCHEMA)
        val catalog = writeFile(name = "catalog.json", content = INVALID_CATALOG)
        val testSubject = rootProject()
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)
        testSubject.featureFlag.schema.set(schema)
        testSubject.featureFlag.catalog.set(catalog)

        // Act & Assert
        assertFailure { testSubject.evaluate() }
            .failureMessages()
            .contains("Feature flag catalog JSON validation failed for ${catalog.path} against ${schema.path}:")
    }

    @Test
    fun `evaluating root project should list every validation error as a detail line`() {
        // Arrange
        val testSubject = rootProject()
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)
        testSubject.featureFlag.schema.set(writeFile(name = "schema.json", content = SCHEMA))
        testSubject.featureFlag.catalog.set(writeFile(name = "catalog.json", content = INVALID_CATALOG))

        // Act & Assert
        assertFailure { testSubject.evaluate() }
            .failureMessages()
            .contains("${System.lineSeparator()}- ")
    }

    @Test
    fun `evaluating root project should succeed when the catalog conforms to the schema`() {
        // Arrange
        val testSubject = rootProject()
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)
        testSubject.featureFlag.schema.set(writeFile(name = "schema.json", content = SCHEMA))
        testSubject.featureFlag.catalog.set(writeFile(name = "catalog.json", content = VALID_CATALOG))

        // Act & Assert
        assertThat(runCatching { testSubject.evaluate() }).isSuccess()
    }

    @Test
    fun `evaluating root project should skip format assertions when validateFormats is false`() {
        // Arrange
        val testSubject = rootProject()
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)
        testSubject.featureFlag.schema.set(writeFile(name = "schema.json", content = SCHEMA))
        testSubject.featureFlag.catalog.set(
            writeFile(name = "catalog.json", content = CATALOG_WITH_INVALID_DATE_FORMAT),
        )
        testSubject.featureFlag.validateFormats.set(false)

        // Act & Assert
        assertThat(runCatching { testSubject.evaluate() }).isSuccess()
    }

    @Test
    fun `evaluating root project should assert formats by default`() {
        // Arrange
        val testSubject = rootProject()
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)
        testSubject.featureFlag.schema.set(writeFile(name = "schema.json", content = SCHEMA))
        testSubject.featureFlag.catalog.set(
            writeFile(name = "catalog.json", content = CATALOG_WITH_INVALID_DATE_FORMAT),
        )

        // Act & Assert
        assertFailure { testSubject.evaluate() }
            .failureMessages()
            .contains("Feature flag catalog JSON validation failed")
    }

    @Test
    fun `evaluating subproject should not validate the catalog`() {
        // Arrange
        val root = rootProject()
        val testSubject = ProjectBuilder.builder()
            .withName("app")
            .withParent(root)
            .build()

        // Act
        testSubject.pluginManager.apply(FeatureFlagPlugin::class.java)

        // Assert
        assertThat(runCatching { testSubject.evaluate() }).isSuccess()
    }

    private fun rootProject(): Project = ProjectBuilder.builder()
        .withProjectDir(temporaryFolder.root)
        .build()

    private fun Project.evaluate() {
        (this as ProjectInternal).evaluate()
    }

    private val Project.featureFlag: FeatureFlagPluginExtension
        get() = extensions.getByType(FeatureFlagPluginExtension::class.java)

    private fun writeFile(name: String, content: String): File =
        temporaryFolder.writeFile(name, content)

    /**
     * Gradle wraps configuration failures, so the assertion has to look at the whole cause chain.
     */
    private fun Assert<Throwable>.failureMessages() = transform { throwable ->
        generateSequence(throwable) { it.cause }
            .mapNotNull { it.message }
            .joinToString(separator = System.lineSeparator())
    }
}
