package net.thunderbird.gradle.plugin.featureflag

import com.github.gmazzo.buildconfig.BuildConfigExtension
import com.github.gmazzo.buildconfig.BuildConfigPlugin
import java.util.Properties
import javax.inject.Inject
import net.thunderbird.gradle.plugin.featureflag.task.registerFeatureFlagKeyEnumsTask
import net.thunderbird.gradle.plugin.featureflag.task.wireGeneratedSourcesIntoKmpSourceSet
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.configure
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
abstract class FeatureFlagLibraryPlugin @Inject constructor(providers: ProviderFactory) : Plugin<Project> {
    override fun apply(target: Project) {
        if (target == target.rootProject.project(FEATURE_FLAG_MODULE_PATH)) {
            val extension = target.extensions.create<FeatureFlagPluginExtension>(FeatureFlagPluginExtension.NAME)
            target.registerKeyEnumGeneration(extension)
            target.setupRemoteFeatureFlagUrl(extension)
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

    private fun Project.setupRemoteFeatureFlagUrl(extension: FeatureFlagPluginExtension) {
        pluginManager.apply(BuildConfigPlugin::class.java)
        configure<BuildConfigExtension> {
            packageName("net.thunderbird.core.featureflag.config")
            val featureFlagRemoteUrl: String? = loadLocalProperties().getProperty(REMOTE_FEATURE_FLAG_URL_KEY)
                ?: providers.gradleProperty(REMOTE_FEATURE_FLAG_URL_KEY).orNull
                ?: throw GradleException("Missing '$REMOTE_FEATURE_FLAG_URL_KEY' definition at gradle.properties")
            buildConfigField(
                type = String::class.java,
                name = "FEATURE_FLAG_REMOTE_URL",
                value = featureFlagRemoteUrl,
            )
            buildConfigField(
                type = String::class.java,
                name = "FEATURE_FLAG_REMOTE_CACHE_FILENAME",
                value = extension.cacheFilename,
            )
        }
    }

    private fun Project.loadLocalProperties(): Properties = Properties().apply {
        @Suppress("UnstableApiUsage")
        val localProperties = isolated.rootProject.projectDirectory.file("local.properties").asFile
        if (localProperties.exists()) {
            localProperties.inputStream().use(::load)
        }
    }

    private companion object {
        const val REMOTE_FEATURE_FLAG_URL_KEY = "tfa.featureflag.remote.url"
        const val KOTLIN_MULTIPLATFORM_PLUGIN_ID = "org.jetbrains.kotlin.multiplatform"
        const val FEATURE_FLAG_MODULE_PATH = ":core:featureflag"
    }
}
