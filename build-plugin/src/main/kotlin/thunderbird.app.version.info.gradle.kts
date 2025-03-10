import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

androidComponents {
    onVariants { variant ->
        val variantName = variant.name.capitalized()
        val printVersionInfoTaskName = "printVersionInfo$variantName"
        tasks.register<PrintVersionInfo>(printVersionInfoTaskName) {
            applicationId.set(variant.applicationId)
            applicationLabel.set(getApplicationLabel(variant))
            versionCode.set(getVersionCode(variant))
            versionName.set(getVersionName(variant))
            versionNameSuffix.set(getVersionNameSuffix(variant))

            // Set outputFile only if provided via -PoutputFile=...
            project.findProperty("outputFile")?.toString()?.let { path ->
                outputFile.set(File(path))
            }

            // Set the `strings.xml` file for the variant to track changes
            findStringsXmlForVariant(variant)?.let { stringsFile ->
                stringsXmlFile.set(project.layout.projectDirectory.file(stringsFile.path))
            }
        }
    }
}

private fun String.capitalized() = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase() else it.toString()
}

abstract class PrintVersionInfo : DefaultTask() {

    @get:Input
    abstract val applicationId: Property<String>

    @get:Input
    abstract val applicationLabel: Property<String>

    @get:Input
    abstract val versionCode: Property<Int>

    @get:Input
    abstract val versionName: Property<String>

    @get:Input
    abstract val versionNameSuffix: Property<String>

    @get:OutputFile
    @get:Optional
    abstract val outputFile: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val stringsXmlFile: RegularFileProperty

    init {
        outputs.upToDateWhen { false } // This forces Gradle to always re-run the task
    }

    @TaskAction
    fun printVersionInfo() {
        val output = """
            APPLICATION_ID=${applicationId.get()}
            APPLICATION_LABEL=${applicationLabel.get()}
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
}

/**
 * Finds the correct `strings.xml` for the given variant.
 */
private fun findStringsXmlForVariant(variant: com.android.build.api.variant.Variant): File? {
    val targetBuildType = variant.buildType ?: return null
    val sourceSets = android.sourceSets

    // Try to find the strings.xml for the specific build type
    val buildTypeSource = sourceSets.findByName(targetBuildType)?.res?.srcDirs?.firstOrNull()
    val stringsXmlFile = buildTypeSource?.resolve("values/strings.xml")

    if (stringsXmlFile?.exists() == true) {
        return stringsXmlFile
    }

    // Fallback to the `main` source set
    val mainSourceSet = sourceSets.findByName("main")?.res?.srcDirs?.firstOrNull()
    return mainSourceSet?.resolve("values/strings.xml")?.takeIf { it.exists() }
}

/**
 * Extracts `APPLICATION_LABEL` from `strings.xml`
 */
private fun getApplicationLabel(variant: com.android.build.api.variant.Variant): Provider<String> {
    return project.provider {
        findStringsXmlForVariant(variant)?.let {
            extractAppName(it)
        } ?: "Unknown"
    }
}

/**
 * Parses `strings.xml` to extract `<string name="app_name">...</string>`
 */
private fun extractAppName(stringsXmlFile: File): String {
    val xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsXmlFile)
    val xPath = XPathFactory.newInstance().newXPath()
    val expression = "/resources/string[@name='app_name']/text()"
    return xPath.evaluate(expression, xmlDocument, XPathConstants.STRING) as String
}

/**
 * Extracts the `VERSION_CODE` from product flavors
 */
private fun getVersionCode(variant: com.android.build.api.variant.Variant): Int {
    val flavorNames = variant.productFlavors.map { it.second }

    val androidExtension =
        project.extensions.findByType(com.android.build.gradle.internal.dsl.BaseAppModuleExtension::class.java)
    val flavor = androidExtension?.productFlavors?.find { it.name in flavorNames }

    return flavor?.versionCode ?: androidExtension?.defaultConfig?.versionCode ?: 0
}

/**
 * Extracts the `VERSION_NAME` from product flavors
 */
private fun getVersionName(variant: com.android.build.api.variant.Variant): String {
    val flavorNames = variant.productFlavors.map { it.second }

    val androidExtension = project.extensions.findByType(
        com.android.build.gradle.internal.dsl.BaseAppModuleExtension::class.java,
    )
    val flavor = androidExtension?.productFlavors?.find { it.name in flavorNames }

    return flavor?.versionName ?: androidExtension?.defaultConfig?.versionName ?: "unknown"
}

/**
 * Extracts the `VERSION_NAME_SUFFIX` from build types
 */
private fun getVersionNameSuffix(variant: com.android.build.api.variant.Variant): String {
    val buildTypeName = variant.buildType ?: return ""
    val androidExtension =
        project.extensions.findByType(com.android.build.gradle.internal.dsl.BaseAppModuleExtension::class.java)
    val buildType = androidExtension?.buildTypes?.find { it.name == buildTypeName }
    return buildType?.versionNameSuffix ?: ""
}
