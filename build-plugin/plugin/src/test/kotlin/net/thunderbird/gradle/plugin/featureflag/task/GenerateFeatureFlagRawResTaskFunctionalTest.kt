package net.thunderbird.gradle.plugin.featureflag.task

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.File
import java.util.Properties
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Functional tests for the Gradle contract of [GenerateFeatureFlagRawResTask]: incremental
 * (up-to-date) behaviour and build cache relocatability.
 *
 * These guarantees come from the task's input/output annotations rather than from its action, so
 * they can only be exercised by running a real build. The unit tests in
 * [GenerateFeatureFlagRawResTaskTest] cover the action itself.
 */
class GenerateFeatureFlagRawResTaskFunctionalTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val buildCacheDir: File by lazy { temporaryFolder.newFolder("build-cache") }

    private val pluginClasspathLiteral: String by lazy {
        val metadata = checkNotNull(javaClass.classLoader.getResourceAsStream(PLUGIN_METADATA_RESOURCE)) {
            "Did not find $PLUGIN_METADATA_RESOURCE on the test classpath."
        }.use { stream -> Properties().apply { load(stream) } }

        metadata.getProperty("implementation-classpath")
            .split(File.pathSeparator)
            .joinToString(separator = ", ") { entry -> "\"${File(entry).invariantSeparatorsPath}\"" }
    }

    @Test
    fun `generateFeatureFlagRawRes task should be up-to-date on the second run when nothing changed`() {
        // Arrange
        val projectDir = createProject(name = "project")

        // Act
        val firstRun = runTask(projectDir)
        val secondRun = runTask(projectDir)

        // Assert
        assertThat(firstRun.outcomeOfTaskUnderTest()).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(secondRun.outcomeOfTaskUnderTest()).isEqualTo(TaskOutcome.UP_TO_DATE)
    }

    @Test
    fun `generateFeatureFlagRawRes task should re-execute when the catalog content changes`() {
        // Arrange
        val projectDir = createProject(name = "project")
        runTask(projectDir)

        // Act
        projectDir.catalogFile().writeText(CHANGED_CATALOG_CONTENT)
        val rerun = runTask(projectDir)

        // Assert
        assertThat(rerun.outcomeOfTaskUnderTest()).isEqualTo(TaskOutcome.SUCCESS)
        assertThat(projectDir.generatedResource().readText()).isEqualTo(CHANGED_CATALOG_CONTENT)
    }

    @Test
    fun `generateFeatureFlagRawRes task should be loaded from the build cache when the project is relocated`() {
        // Arrange
        val originalDir = createProject(name = "original")
        val relocatedDir = createProject(name = "relocated")
        runTask(originalDir)

        // Act
        val relocatedRun = runTask(relocatedDir)

        // Assert
        assertThat(relocatedRun.outcomeOfTaskUnderTest()).isEqualTo(TaskOutcome.FROM_CACHE)
        assertThat(relocatedDir.generatedResource().readText()).isEqualTo(CATALOG_CONTENT)
    }

    /**
     * Creates a standalone Gradle build that registers the task under test. Every project shares the
     * same [buildCacheDir] and the same root project name so that the only difference between two
     * projects is their location on disk.
     */
    private fun createProject(name: String): File {
        val projectDir = temporaryFolder.newFolder(name)

        projectDir.resolve("settings.gradle.kts").writeText(
            // language=kotlin
            """
            rootProject.name = "feature-flag-catalog"

            buildCache {
                local {
                    directory = file("${buildCacheDir.invariantSeparatorsPath}")
                }
            }
            """.trimIndent(),
        )

        projectDir.resolve("build.gradle.kts").writeText(
            // language=kotlin
            """
            import net.thunderbird.gradle.plugin.featureflag.task.GenerateFeatureFlagRawResTask

            buildscript {
                dependencies {
                    classpath(files($pluginClasspathLiteral))
                }
            }

            tasks.register<GenerateFeatureFlagRawResTask>("${GenerateFeatureFlagRawResTask.TASK_NAME}") {
                catalog.set(layout.projectDirectory.file("$CATALOG_PATH"))
                resourceName.set("$RESOURCE_NAME")
                outputResDir.set(layout.buildDirectory.dir("$OUTPUT_RES_DIR"))
            }
            """.trimIndent(),
        )

        projectDir.catalogFile()
            .apply { parentFile.mkdirs() }
            .writeText(CATALOG_CONTENT)

        return projectDir
    }

    private fun runTask(projectDir: File): BuildResult = GradleRunner.create()
        .withProjectDir(projectDir)
        .withArguments(GenerateFeatureFlagRawResTask.TASK_NAME, "--build-cache")
        .build()

    private fun BuildResult.outcomeOfTaskUnderTest(): TaskOutcome? =
        task(":${GenerateFeatureFlagRawResTask.TASK_NAME}")?.outcome

    private fun File.catalogFile(): File = resolve(CATALOG_PATH)

    private fun File.generatedResource(): File = resolve("build/$OUTPUT_RES_DIR/raw/$RESOURCE_NAME.json")

    private companion object {
        const val PLUGIN_METADATA_RESOURCE = "plugin-under-test-metadata.properties"
        const val CATALOG_PATH = "config/featureflag/thunderbird_mobile_featureflag.catalog.json"
        const val OUTPUT_RES_DIR = "generated/featureflags/res"
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

        // language=json
        val CHANGED_CATALOG_CONTENT = """
            {
              "version": "2026-07-31.1",
              "flags": [
                {
                  "key": "archive_marks_as_read",
                  "default": false
                }
              ],
              "overrides": {}
            }
        """.trimIndent()
    }
}
