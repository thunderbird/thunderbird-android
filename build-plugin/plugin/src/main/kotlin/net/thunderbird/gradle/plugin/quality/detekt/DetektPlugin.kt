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
            // CI can pass -Ptfa.detekt.config=<path> to run with a narrower config, see detekt-ci-type-resolution.yml
            val configPath = providers.gradleProperty(DETEKT_CONFIG_PROPERTY).getOrElse(DEFAULT_DETEKT_CONFIG)
            config.setFrom(isolated.rootProject.projectDirectory.file(configPath).asFile)

            ignoredBuildTypes = listOf("release")
        }
    }

    private fun Project.configureDetektTasks() {
        with(tasks) {
            withType<Detekt>().configureEach {
                val isInProjectBuildDirectory = buildDirectoryExclusion(layout.buildDirectory.get().asFile)

                if (name.contains("androidHostTest", ignoreCase = true)) {
                    enabled = false
                }

                jvmTarget = ProjectConfig.Compiler.jvmTarget.target

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

                if (name.contains("androidHostTest", ignoreCase = true)) {
                    enabled = false
                }

                jvmTarget = ProjectConfig.Compiler.jvmTarget.target

                exclude(isInProjectBuildDirectory)
            }

            register("detektAll") {
                group = "verification"
                description = "Runs detekt on this project"

                dependsOn(tasks.withType<Detekt>())
            }
        }
    }
}

private const val DETEKT_CONFIG_PROPERTY = "tfa.detekt.config"
private const val DEFAULT_DETEKT_CONFIG = "config/detekt/detekt.yml"

private fun buildDirectoryExclusion(buildDirectory: File): (FileTreeElement) -> Boolean {
    val buildDirectoryPath = buildDirectory.normalizedPath()

    return { fileTreeElement ->
        fileTreeElement.file.normalizedPath().startsWith(buildDirectoryPath)
    }
}

private fun File.normalizedPath(): Path = absoluteFile.toPath().normalize()
