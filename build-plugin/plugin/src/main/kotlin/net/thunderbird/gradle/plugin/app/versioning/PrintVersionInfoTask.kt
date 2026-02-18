package net.thunderbird.gradle.plugin.app.versioning

import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

abstract class PrintVersionInfoTask : DefaultTask() {
    @get:Input
    abstract val applicationId: Property<String>

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:InputFiles
    abstract val resourceFiles: ConfigurableFileCollection

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionNameSuffix: Property<String>

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    init {
        outputs.upToDateWhen { false } // This forces Gradle to always re-run the task
    }

    @TaskAction
    fun printVersionInfo() {
        val label = getApplicationLabel()
        val output = """
            APPLICATION_ID=${applicationId.get()}
            APPLICATION_LABEL=$label
            VERSION_CODE=${versionCode.get()}
            VERSION_NAME=${versionName.get()}
            VERSION_NAME_SUFFIX=${versionNameSuffix.get()}
            FULL_VERSION_NAME=${versionName.get()}${versionNameSuffix.get()}
        """.trimIndent()

        println(output)

        if (outputFile.isPresent) {
            outputFile.get().asFile.writeText(output + "\n")
        }
    }

    private fun getApplicationLabel(): String {
        val manifestFile = mergedManifest.get().asFile
        if (!manifestFile.exists()) return "Unknown"

        val labelRaw = readManifestApplicationLabel(manifestFile) ?: return "Unknown"

        // Return raw label if not a resource string
        val match = STRING_RESOURCE_REGEX.matchEntire(labelRaw.trim()) ?: return labelRaw
        val resourceName = match.groupValues[1]

        val resolvedApplicationLabel = resourceFiles
            .map { it }
            .mapNotNull { dir -> File(dir, "values/strings.xml").takeIf { it.exists() } }
            .firstNotNullOfOrNull { stringResourceFile -> readStringResource(stringResourceFile, resourceName) }

        return resolvedApplicationLabel ?: "Unknown"
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

    private fun readStringResource(stringResourceFile: File, resourceName: String): String? {
        val xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringResourceFile)
        val xPath = XPathFactory.newInstance().newXPath()
        val expression = "/resources/string[@name='$resourceName']/text()"
        val value = xPath.evaluate(expression, xmlDocument, XPathConstants.STRING) as String
        return value.trim().takeIf { it.isNotEmpty() }
    }

    private companion object {
        val STRING_RESOURCE_REGEX = "^@string/([A-Za-z0-9_]+)$".toRegex()
    }
}
