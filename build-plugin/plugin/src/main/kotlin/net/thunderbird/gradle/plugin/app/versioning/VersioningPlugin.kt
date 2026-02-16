package net.thunderbird.gradle.plugin.app.versioning

import com.android.build.api.artifact.SingleArtifact
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.ApplicationVariant
import com.android.build.api.variant.Variant
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
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
                    val versionInfo = getVersionInfo(variant).get()

                    applicationId = variant.applicationId
                    applicationLabel = getApplicationLabel(variant)
                    versionCode = versionInfo.versionCode
                    versionName = versionInfo.versionName
                    versionNameSuffix = versionInfo.versionNameSuffix

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

    private fun Project.getApplicationLabel(variant: Variant): Provider<String> {
        val mergedManifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)

        return providers.zip(mergedManifest, provider { variant }) { mergedManifest, _ ->
            val labelRaw = readManifestApplicationLabel(mergedManifest.asFile) ?: return@zip "Unknown"

            // Return raw label if not a resource string
            val match = STRING_RESOURCE_REGEX.matchEntire(labelRaw.trim()) ?: return@zip labelRaw
            val resourceName = match.groupValues[1]

            val resourceDirs = variant.sources.res?.all?.get()?.filter { it.isNotEmpty() }?.flatten() ?: emptyList()

            val resolvedApplicationLabel = resourceDirs
                .map { it.asFile }
                .mapNotNull { dir -> File(dir, "values/strings.xml").takeIf { it.exists() } }
                .firstNotNullOfOrNull { stringResourceFile -> readStringResource(stringResourceFile, resourceName) }

            resolvedApplicationLabel ?: "Unknown"
        }
    }

    private fun readManifestApplicationLabel(manifest: File): String? {
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(manifest)

        val apps = document.getElementsByTagName("application")
        if (apps.length == 0) return null

        val appElement = apps.item(0)
        return appElement.attributes?.getNamedItemNS("http://schemas.android.com/apk/res/android", "label")?.nodeValue
            ?: appElement.attributes?.getNamedItem("android:label")?.nodeValue
            ?: appElement.attributes?.getNamedItem("label")?.nodeValue
    }

    /**
     * Parses stringResourceFile to extract `<string name="resourceName">...</string>`
     */
    private fun readStringResource(stringResourceFile: File, resourceName: String): String? {
        val xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringResourceFile)
        val xPath = XPathFactory.newInstance().newXPath()
        val expression = "/resources/string[@name='$resourceName']/text()"
        val value = xPath.evaluate(expression, xmlDocument, XPathConstants.STRING) as String
        return value.trim().takeIf { it.isNotEmpty() }
    }

    private fun String.capitalized() = replaceFirstChar {
        if (it.isLowerCase()) it.titlecase() else it.toString()
    }

    private companion object {
        val STRING_RESOURCE_REGEX = "^@string/([A-Za-z0-9_]+)$".toRegex()
    }
}


