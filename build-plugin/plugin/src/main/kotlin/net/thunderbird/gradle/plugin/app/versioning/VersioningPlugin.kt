package net.thunderbird.gradle.plugin.app.versioning

import com.android.build.api.artifact.SingleArtifact
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import java.io.File
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register

class VersioningPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("com.android.application")
            }

            configureVersioning()
        }
    }

    private fun Project.configureVersioning() {
        extensions.configure<ApplicationAndroidComponentsExtension> {
            onVariants { variant ->
                val variantName = variant.name.capitalized()
                val printVersionInfoTaskName = "printVersionInfo$variantName"

                tasks.register<PrintVersionInfoTask>(printVersionInfoTaskName) {
                    val versionInfoProvider = getVersionInfo(variant)

                    applicationId = variant.applicationId
                    mergedManifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
                    resourceFiles.from(variant.sources.res?.all)
                    versionCode = versionInfoProvider.map { it.versionCode }
                    versionName = versionInfoProvider.map { it.versionName }
                    versionNameSuffix = versionInfoProvider.map { it.versionNameSuffix }

                    // Set outputFile only if provided via -PoutputFile=...
                    project.findProperty("outputFile")?.toString()?.let { path ->
                        outputFile.set(File(path))
                    }
                }
            }
        }
    }

    /**
     * Get version information for the given variant.
     */
    private fun Project.getVersionInfo(variant: ApplicationVariant): Provider<VersionInfo> {
        return provider {
            val flavorNames = variant.productFlavors.map { it.second }
            val androidExtension = extensions.findByType(BaseAppModuleExtension::class.java)
            val flavor = androidExtension?.productFlavors?.find { it.name in flavorNames }
            val builtType = androidExtension?.buildTypes?.find { it.name == variant.buildType }

            val versionCode = flavor?.versionCode ?: androidExtension?.defaultConfig?.versionCode ?: 0
            val versionName = flavor?.versionName ?: androidExtension?.defaultConfig?.versionName ?: "unknown"
            val versionNameSuffix = builtType?.versionNameSuffix.orEmpty()

            VersionInfo(
                versionCode = versionCode,
                versionName = versionName,
                versionNameSuffix = versionNameSuffix,
            )
        }
    }

    private fun String.capitalized() = replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }
}


