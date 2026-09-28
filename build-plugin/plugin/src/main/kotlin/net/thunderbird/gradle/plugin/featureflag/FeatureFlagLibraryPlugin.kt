package net.thunderbird.gradle.plugin.featureflag

import net.thunderbird.gradle.plugin.featureflag.task.registerFeatureFlagKeyEnumsTask
import net.thunderbird.gradle.plugin.featureflag.task.wireGeneratedSourcesIntoKmpSourceSet
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

/**
 * A Gradle plugin that generates feature flag key enums from a feature flag catalog.
 *
 * This plugin should be applied to the feature flag module (`:core:featureflag`) in a multi-module
 * project.
 *
 * @throws GradleException if the plugin is applied to other project then `:core:featureflag`
 */
@Suppress("unused")
abstract class FeatureFlagLibraryPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        if (target == target.rootProject.project(FEATURE_FLAG_MODULE_PATH)) {
            val extension = target.extensions.create<FeatureFlagPluginExtension>(FeatureFlagPluginExtension.NAME)
            target.registerKeyEnumGeneration(extension)
        } else {
            throw GradleException("This plugin must be applied only on $FEATURE_FLAG_MODULE_PATH")
        }
    }

    /**
     * Registers the key-enum generation task on the project this plugin is applied to.
     *
     * The generated enum has a fixed, global fully-qualified name, so it must be compiled exactly
     * once. Applying the plugin to the module that every consumer already depends on lets the single
     * class flow transitively to all of them, avoiding duplicate-class failures at dex merge.
     */
    private fun Project.registerKeyEnumGeneration(extension: FeatureFlagPluginExtension) {
        logger.debug("[feature-flag] Registering the key enum generation on {}", this)
        val taskProvider = registerFeatureFlagKeyEnumsTask(extension)
        pluginManager.withPlugin(KOTLIN_MULTIPLATFORM_PLUGIN_ID) {
            wireGeneratedSourcesIntoKmpSourceSet(taskProvider)
        }
    }

    private companion object {
        const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
        const val FEATURE_FLAG_MODULE_PATH = ":core:featureflag"
    }
}
