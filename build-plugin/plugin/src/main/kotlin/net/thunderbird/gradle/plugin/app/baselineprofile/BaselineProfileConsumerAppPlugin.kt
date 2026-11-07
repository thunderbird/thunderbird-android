package net.thunderbird.gradle.plugin.app.baselineprofile

import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import net.thunderbird.gradle.plugin.extension.baselineProfileForConsumer
import net.thunderbird.gradle.plugin.libs
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/**
 * Gradle plugin that configures an application module as a baseline profile consumer.
 *
 * It applies the AndroidX baseline profile plugin, adds the `profileinstaller` dependency, and merges the
 * profiles generated for all variants into the main source set so a single profile is used by all variants.
 */
// suppressed unused since this is used by the Gradle build system.
@Suppress("Unused")
abstract class BaselineProfileConsumerAppPlugin : Plugin<Project> {
    override fun apply(target: Project): Unit = with(target) {
        pluginManager.apply(libs.plugins.androidx.baselineprofile.get().pluginId)

        dependencies {
            add("implementation", libs.androidx.profileinstaller)
        }
        baselineProfileForConsumer {
            automaticGenerationDuringBuild = false
            mergeIntoMain = true
            dexLayoutOptimization = true
        }

        // The baseline profile plugin adds a `benchmark*` and `nonMinified*` build type for every release-like build
        // type. They are only needed to generate profiles or run benchmarks, so skip them unless requested.
        if (!isBenchmarkEnabled()) {
            baselineProfileForConsumer {
                // The variants below are disabled on purpose
                warnings.disabledVariants = false
            }

            extensions.configure<ApplicationAndroidComponentsExtension> {
                beforeVariants { variant ->
                    val buildType = variant.buildType.orEmpty()
                    if (buildType.startsWith(BENCHMARK_BUILD_TYPE_PREFIX) ||
                        buildType.startsWith(NON_MINIFIED_BUILD_TYPE_PREFIX)
                    ) {
                        variant.enable = false
                    }
                }
            }

            // Without the producer variants, generating would only merge nothing into the committed profiles
            tasks.matching { it.name.startsWith("generate") && it.name.endsWith("BaselineProfile") }.configureEach {
                doFirst {
                    throw GradleException("Baseline profile generation requires -Pbenchmark=true")
                }
            }
        }
    }

    private companion object {
        const val BENCHMARK_BUILD_TYPE_PREFIX = "benchmark"
        const val NON_MINIFIED_BUILD_TYPE_PREFIX = "nonMinified"
    }
}
