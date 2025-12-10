package net.thunderbird.gradle.plugin.quality.coverage

import kotlinx.kover.gradle.plugin.KoverGradlePlugin
import kotlinx.kover.gradle.plugin.dsl.AggregationType
import kotlinx.kover.gradle.plugin.dsl.CoverageUnit
import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig
import kotlinx.kover.gradle.plugin.dsl.KoverVerificationRulesConfig
import net.thunderbird.gradle.plugin.quality.coverage.filter.androidFilter
import net.thunderbird.gradle.plugin.quality.coverage.filter.commonFilter
import net.thunderbird.gradle.plugin.quality.coverage.filter.composeFilter
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create

/**
 * A Gradle plugin that configures code coverage using the Kover plugin.
 *
 * It sets up default coverage thresholds and allows for customization via the [CodeCoverageExtension].
 *
 * The plugin can be globally disabled using a Gradle property or environment variable:
 *  - Gradle property: `-PcodeCoverageDisabled=true`
 *  - Environment variable: `CODE_COVERAGE_DISABLED=true`
 *
 * Example usage in a build script:
 *
 * ```kotlin
 * plugins {
 *    id("net.thunderbird.gradle.plugin.quality.coverage")
 * }
 *
 * codeCoverage {
 *    disabled.set(false) // Enable or disable coverage
 *    lineCoverage.set(80) // Set line coverage threshold
 *    branchCoverage.set(70) // Set branch coverage threshold
 * }
 */
class CodeCoveragePlugin: Plugin<Project> {

    override fun apply(target: Project) {
        val extension = target.extensions.create<CodeCoverageExtension>("codeCoverage")

        val gradleProperty = target.providers.gradleProperty("codeCoverageDisabled").map { it.toBoolean() }
        val environmentProperty = target.providers.environmentVariable("CODE_COVERAGE_DISABLED")
            .map { it.equals("true", ignoreCase = true) }
        val disabledProvider = environmentProperty.orElse(gradleProperty).orElse(false)

        extension.disabled.convention(disabledProvider)

        extension.initialize()
        extension.finalizeValueOnRead()

        target.pluginManager.apply(KoverGradlePlugin::class)
        target.configureKover(extension)
    }

    private fun Project.configureKover(coverageExtension: CodeCoverageExtension) {
        extensions.configure<KoverProjectExtension>("kover") {
            if (coverageExtension.disabled.get()) {
                disable()
            }

            // See https://www.jacoco.org/jacoco/
            useJacoco("0.8.14")

            reports {
                total {
                    filters {
                        commonFilter()
                        composeFilter()
                        androidFilter()
                    }
                }

                verify {
                    warningInsteadOfFailure.set(false)

                    applyVerificationRules(coverageExtension)
                }
            }
        }
    }

    private fun KoverVerificationRulesConfig.applyVerificationRules(coverageExtension: CodeCoverageExtension) {
        rule("branchCoveragePercentage") {
            disabled.set(coverageExtension.disabled)
            bound {
                minValue.set(coverageExtension.branchCoverage)
                coverageUnits.set(CoverageUnit.BRANCH)
                aggregationForGroup.set(AggregationType.COVERED_PERCENTAGE)
            }
        }
        rule("lineCoveragePercentage") {
            disabled.set(coverageExtension.disabled)
            bound {
                minValue.set(coverageExtension.lineCoverage)
                coverageUnits.set(CoverageUnit.LINE)
                aggregationForGroup.set(AggregationType.COVERED_PERCENTAGE)
            }
        }
    }
}
