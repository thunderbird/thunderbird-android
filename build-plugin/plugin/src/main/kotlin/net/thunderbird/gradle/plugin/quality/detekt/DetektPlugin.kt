package net.thunderbird.gradle.plugin.quality.detekt

import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.DetektCreateBaselineTask
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import net.thunderbird.gradle.plugin.ProjectConfig
import net.thunderbird.gradle.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
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
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            // Access libs extension lazily since it might not be available yet when the plugin is applied
            // (especially in precompiled script plugins). Could be removed once all precompiled script plugins
            // are migrated to the new plugin model.
            afterEvaluate {
                dependencies {
                    add("detektPlugins", libs.detekt.plugin.compose)
                }
            }

            if (this == rootProject) {
                configureRootDetektTasks()
            } else {
                configureDetekt()
                configureDetektTasks()
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun Project.configureDetekt() {
        extensions.configure<DetektExtension>("detekt") {
            config.setFrom(isolated.rootProject.projectDirectory.file("config/detekt/detekt.yml").asFile)

            val name = project.path.replace(":", "-").replace("/", "-")
            baseline = isolated.rootProject.projectDirectory
                .file("config/detekt/detekt-baseline$name.xml").asFile

            ignoredBuildTypes = listOf("release")
        }
    }

    private fun Project.configureDetektTasks() {
        with(tasks) {
            withType<Detekt>().configureEach {
                if (name.contains("androidHostTest", ignoreCase = true)) {
                    enabled = false
                }

                jvmTarget = ProjectConfig.Compiler.jvmTarget.target

                exclude(defaultExcludes)

                reports {
                    html.required.set(true)
                    sarif.required.set(true)
                    xml.required.set(true)
                }

                tasks.getByName("build").dependsOn(this)
            }

            withType<DetektCreateBaselineTask>().configureEach {
                if (name.contains("androidHostTest", ignoreCase = true)) {
                    enabled = false
                }

                jvmTarget = ProjectConfig.Compiler.jvmTarget.target

                exclude(defaultExcludes)
            }

            register("detektAll") {
                group = "verification"
                description = "Runs detekt on this project"

                dependsOn(tasks.withType<Detekt>())
            }
        }
    }

    private fun Project.configureRootDetektTasks() {
        with(tasks) {
            register("detektAll") {
                group = "verification"
                description = "Runs detekt on the whole project"

                allprojects {
                    this@register.dependsOn(tasks.withType<Detekt>())
                }
            }
        }
    }
}

private val defaultExcludes = listOf(
    "**/.gradle/**",
    "**/.idea/**",
    "**/build/**",
    "**/generated/**",
    ".github/**",
    "gradle/**",
)
