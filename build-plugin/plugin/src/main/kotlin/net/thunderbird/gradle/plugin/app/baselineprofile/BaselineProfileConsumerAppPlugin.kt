package net.thunderbird.gradle.plugin.app.baselineprofile

import net.thunderbird.gradle.plugin.extension.baselineProfileForConsumer
import net.thunderbird.gradle.plugin.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
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
    }
}
