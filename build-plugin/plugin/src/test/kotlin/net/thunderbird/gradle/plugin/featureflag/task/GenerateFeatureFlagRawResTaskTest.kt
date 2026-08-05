package net.thunderbird.gradle.plugin.featureflag.task

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import java.io.File
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class GenerateFeatureFlagRawResTaskTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `generate should copy the catalog into the raw directory`() {
        // Arrange
        val testSubject = createTestSubject(resourceName = RESOURCE_NAME)

        // Act
        testSubject.generate()

        // Assert
        val rawDir = testSubject.rawDir()
        assertThat(rawDir.listFiles().orEmpty().map(File::getName)).isEqualTo(listOf("$RESOURCE_NAME.json"))
        assertThat(rawDir.resolve("$RESOURCE_NAME.json").readText()).isEqualTo(CATALOG_CONTENT)
    }

    @Test
    fun `generate should name the copied file after the resource name`() {
        // Arrange
        val testSubject = createTestSubject(resourceName = "featureflag_catalog_lean")

        // Act
        testSubject.generate()

        // Assert
        val rawDir = testSubject.rawDir()
        assertThat(rawDir.listFiles().orEmpty().map(File::getName)).isEqualTo(listOf("featureflag_catalog_lean.json"))
    }

    @Test
    fun `generate should sanitize dashes and dots in the resource name`() {
        // Arrange
        val testSubject = createTestSubject(resourceName = "thunderbird-mobile-featureflag.catalog")

        // Act
        testSubject.generate()

        // Assert
        val rawDir = testSubject.rawDir()
        assertThat(rawDir.listFiles().orEmpty().map(File::getName))
            .isEqualTo(listOf("thunderbird_mobile_featureflag_catalog.json"))
    }

    @Test
    fun `generate should remove resources left over by a previous run`() {
        // Arrange
        val testSubject = createTestSubject(resourceName = RESOURCE_NAME)
        val staleResource = testSubject.rawDir()
            .apply { mkdirs() }
            .resolve("featureflag_catalog_stale.json")
            .apply { writeText("{}") }

        // Act
        testSubject.generate()

        // Assert
        assertThat(staleResource.exists()).isFalse()
        assertThat(testSubject.rawDir().resolve("$RESOURCE_NAME.json").readText()).isEqualTo(CATALOG_CONTENT)
    }

    private fun createTestSubject(resourceName: String): GenerateFeatureFlagRawResTask {
        val catalogFile = temporaryFolder.newFile("thunderbird_mobile_featureflag.catalog.json")
            .apply { writeText(CATALOG_CONTENT) }
        val project = ProjectBuilder.builder()
            .withProjectDir(temporaryFolder.newFolder("project"))
            .build()

        return project.tasks
            .register(GenerateFeatureFlagRawResTask.TASK_NAME, GenerateFeatureFlagRawResTask::class.java)
            .get()
            .apply {
                catalog.set(catalogFile)
                this.resourceName.set(resourceName)
                outputResDir.set(project.layout.buildDirectory.dir("generated/featureflags/res"))
            }
    }

    private fun GenerateFeatureFlagRawResTask.rawDir(): File = outputResDir.get().asFile.resolve("raw")

    private companion object {
        const val RESOURCE_NAME = "thunderbird_mobile_featureflag_catalog"

        // language=json
        val CATALOG_CONTENT = """
            {
              "version": "2026-07-30.1",
              "flags": [
                {
                  "key": "archive_marks_as_read",
                  "default": true
                }
              ],
              "overrides": {}
            }
        """.trimIndent()
    }
}
