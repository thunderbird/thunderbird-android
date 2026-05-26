package net.thunderbird.gradle.plugin.quality.spotless

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * A Gradle plugin to configure Spotless code formatting for Kotlin, Kotlin Gradle scripts, Markdown,
 * and miscellaneous files like .gitignore.
 */
class SpotlessPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.diffplug.spotless")

            if (this == rootProject) {
                configureSpotlessRoot()
            } else {
                configureSpotless()
            }
        }
    }

    private fun Project.configureSpotless() {
        extensions.configure<SpotlessExtension> {
            kotlin {
                target(
                    "src/*/kotlin/*.kt",
                    "src/*/kotlin/**/*.kt",
                )

                ktlint()
                    .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
                    .editorConfigOverride(kotlinEditorConfigOverride)
            }

            kotlinGradle {
                target(
                    "*.gradle.kts",
                )

                ktlint()
                    .setEditorConfigPath("${rootProject.projectDir}/.editorconfig")
                    .editorConfigOverride(
                        mapOf(
                            "ktlint_code_style" to "intellij_idea",
                            "ktlint_standard_function-expression-body" to "disabled",
                            "ktlint_standard_function-signature" to "disabled",
                        ),
                    )
            }

            flexmark {
                target(
                    "*.md",
                )
                flexmark()
            }

            format("misc") {
                target(".gitignore")
                trimTrailingWhitespace()
            }
        }
    }

    private fun Project.configureSpotlessRoot() {
        extensions.configure<SpotlessExtension> {
            kotlin {
                target(
                    "build-plugin/plugin/src/*/kotlin/*.kt",
                    "build-plugin/plugin/src/*/kotlin/**/*.kt",
                )
                ktlint()
                    .setEditorConfigPath("${project.rootProject.projectDir}/.editorconfig")
                    .editorConfigOverride(kotlinEditorConfigOverride)
            }

            kotlinGradle {
                target(
                    "*.gradle.kts",
                    "build-plugin/*.gradle.kts",
                    "build-plugin/plugin/*.gradle.kts",
                )

                ktlint()
                    .setEditorConfigPath("${project.rootProject.projectDir}/.editorconfig")
                    .editorConfigOverride(
                        mapOf(
                            "ktlint_code_style" to "intellij_idea",
                            "ktlint_standard_function-expression-body" to "disabled",
                            "ktlint_standard_function-signature" to "disabled",
                        ),
                    )
            }

            flexmark {
                target(
                    "*.md",
                    "docs/*.md",
                    "docs/**/*.md",
                )
                flexmark()
            }

            format("misc") {
                target(".gitignore")
                trimTrailingWhitespace()
            }
        }
    }
}
