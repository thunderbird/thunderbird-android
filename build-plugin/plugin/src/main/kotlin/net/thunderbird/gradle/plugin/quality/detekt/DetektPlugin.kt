package net.thunderbird.gradle.plugin.quality.detekt

import dev.detekt.gradle.Detekt
import dev.detekt.gradle.DetektCreateBaselineTask
import dev.detekt.gradle.extensions.DetektExtension
import java.io.File
import java.nio.file.Path
import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Detekt plugin configuration.
 *
 * Applies the Detekt plugin, sets up configuration, and defines tasks for static code analysis.
 */
class DetektPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("dev.detekt")

            // Access libs extension lazily since it might not be available yet when the plugin is applied
            // (especially in precompiled script plugins). Could be removed once all precompiled script plugins
            // are migrated to the new plugin model.
            afterEvaluate {
                dependencies {
                    add("detektPlugins", libs.detekt.plugin.compose)
                }
            }

            configureDetekt()
            configureDetektTasks()
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Project.configureDetekt() {
        extensions.configure<DetektExtension>("detekt") {
            config.setFrom(isolated.rootProject.projectDirectory.file("config/detekt/detekt.yml").asFile)

            ignoredBuildTypes = listOf("release")
        }
    }

    private fun Project.configureDetektTasks() {
        with(tasks) {
            withType<Detekt>().configureEach {
                val isInProjectBuildDirectory = buildDirectoryExclusion(layout.buildDirectory.get().asFile)

                if (isAndroidHostTest(name)) {
                    enabled = false
                }

                jvmTarget = ProjectConfig.Compiler.jvmTarget.target

                if (isTypeResolved(name)) {
                    config.setFrom(typeResolutionConfigFile())
                }

                exclude(isInProjectBuildDirectory)

                reports {
                    checkstyle.required.set(false)
                    html.required.set(false)
                    sarif.required.set(true)
                    markdown.required.set(true)
                }

                tasks.getByName("build").dependsOn(this)
            }

            withType<DetektCreateBaselineTask>().configureEach {
                val isInProjectBuildDirectory = buildDirectoryExclusion(layout.buildDirectory.get().asFile)

                if (isAndroidHostTest(name)) {
                    enabled = false
                }

                jvmTarget = ProjectConfig.Compiler.jvmTarget.target

                if (isTypeResolved(name)) {
                    config.setFrom(typeResolutionConfigFile())
                }

                exclude(isInProjectBuildDirectory)
            }

            // Plain detekt only analyzes src/main and src/test, which KMP modules don't have, so it has to run the
            // source set tasks instead.
            pluginManager.withPlugin("org.jetbrains.kotlin.multiplatform") {
                named(DETEKT_TASK_NAME) {
                    dependsOn(withType<Detekt>().matching { it.name.endsWith(SOURCE_SET_TASK_SUFFIX) })
                }
            }

            register("detektAll") {
                group = "verification"
                description = "Runs detekt on this project"

                dependsOn(tasks.withType<Detekt>())
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Project.typeResolutionConfigFile(): File =
        isolated.rootProject.projectDirectory.file(TYPE_RESOLUTION_CONFIG).asFile
}

private const val DETEKT_TASK_NAME = "detekt"
private const val DETEKT_BASELINE_TASK_NAME = "detektBaseline"
private const val SOURCE_SET_TASK_SUFFIX = "SourceSet"

// Type-resolved rules still report a backlog of existing findings with detekt.yml, so the type-resolved tasks only run
// the rules enabled in this config.
private const val TYPE_RESOLUTION_CONFIG = "config/detekt/detekt-type-resolution.yml"

/**
 * Matches both the source set tasks (`detektAndroidHostTestSourceSet`) and the type-resolved tasks
 * (`detektHostTestAndroid`) for Android host tests.
 */
private fun isAndroidHostTest(taskName: String): Boolean =
    taskName.contains("androidHostTest", ignoreCase = true) ||
        taskName.contains("hostTestAndroid", ignoreCase = true)

/**
 * The plain `detekt` task and the `detekt<SourceSet>SourceSet` tasks run without type resolution. All other detekt
 * tasks (`detekt<Variant>`, e.g. `detektDebugUnitTest` or `detektMainJvm`) are type-resolved.
 */
private fun isTypeResolved(taskName: String): Boolean =
    taskName != DETEKT_TASK_NAME &&
        taskName != DETEKT_BASELINE_TASK_NAME &&
        !taskName.endsWith(SOURCE_SET_TASK_SUFFIX)

private fun buildDirectoryExclusion(buildDirectory: File): (FileTreeElement) -> Boolean {
    val buildDirectoryPath = buildDirectory.normalizedPath()

    return { fileTreeElement ->
        fileTreeElement.file.normalizedPath().startsWith(buildDirectoryPath)
    }
}

private fun File.normalizedPath(): Path = absoluteFile.toPath().normalize()
