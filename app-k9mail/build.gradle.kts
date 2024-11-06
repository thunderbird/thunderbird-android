import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

plugins {
    id(ThunderbirdPlugins.App.androidCompose)
    alias(libs.plugins.dependency.guard)
    id("thunderbird.quality.badging")
}

val testCoverageEnabled: Boolean by extra
if (testCoverageEnabled) {
    apply(plugin = "jacoco")
}

dependencies {
    implementation(projects.appCommon)
    implementation(projects.core.ui.compose.theme2.k9mail)
    implementation(projects.core.ui.legacy.theme2.k9mail)
    implementation(projects.feature.launcher)

    implementation(projects.legacy.core)
    implementation(projects.legacy.ui.legacy)

    implementation(projects.core.featureflags)

    implementation(projects.feature.widget.messageList)
    implementation(projects.feature.widget.shortcut)
    implementation(projects.feature.widget.unread)
    implementation(projects.feature.telemetry.noop)
    implementation(projects.feature.funding.noop)
    implementation(projects.feature.onboarding.migration.noop)
    implementation(projects.feature.migration.launcher.noop)

    implementation(libs.androidx.work.runtime)

    implementation(projects.feature.autodiscovery.api)
    debugImplementation(projects.backend.demo)
    debugImplementation(projects.feature.autodiscovery.demo)

    testImplementation(libs.robolectric)

    // Required for DependencyInjectionTest to be able to resolve OpenPgpApiManager
    testImplementation(projects.plugins.openpgpApiLib.openpgpApi)
    testImplementation(projects.feature.account.setup)
}

android {
    namespace = "com.fsck.k9"

    defaultConfig {
        applicationId = "com.fsck.k9"
        testApplicationId = "com.fsck.k9.tests"

        versionCode = 39012
        versionName = "8.1"

        // Keep in sync with the resource string array "supported_languages"
        resourceConfigurations.addAll(
            listOf(
                "ar",
                "be",
                "bg",
                "ca",
                "co",
                "cs",
                "cy",
                "da",
                "de",
                "el",
                "en",
                "en_GB",
                "eo",
                "es",
                "et",
                "eu",
                "fa",
                "fi",
                "fr",
                "fy",
                "ga",
                "gl",
                "hr",
                "hu",
                "in",
                "is",
                "it",
                "iw",
                "ja",
                "ko",
                "lt",
                "lv",
                "nb",
                "nl",
                "nn",
                "pl",
                "pt_BR",
                "pt_PT",
                "ro",
                "ru",
                "sl",
                "sq",
                "sr",
                "sv",
                "tr",
                "uk",
                "vi",
                "zh_CN",
                "zh_TW",
            ),
        )

        buildConfigField("String", "CLIENT_INFO_APP_NAME", "\"K-9 Mail\"")
    }

    signingConfigs {
        createSigningConfig(project, SigningType.K9_RELEASE, isUpload = false)
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByType(SigningType.K9_RELEASE)

            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )
        }

        debug {
            applicationIdSuffix = ".debug"
            enableUnitTestCoverage = testCoverageEnabled
            enableAndroidTestCoverage = testCoverageEnabled

            isMinifyEnabled = false
        }
    }

    packaging {
        jniLibs {
            excludes += listOf("kotlin/**")
        }

        resources {
            excludes += listOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "kotlin/**",
                "DebugProbesKt.bin",
            )
        }
    }
}

dependencyGuard {
    configuration("releaseRuntimeClasspath")
}

tasks.create("printVersionInfo") {
    val targetBuildType = project.findProperty("buildType") ?: "debug"

    doLast {
        android.applicationVariants.all { variant ->
            if (variant.buildType.name == targetBuildType) {
                val flavor = variant.mergedFlavor

                var buildTypeSource = android.sourceSets.getByName(targetBuildType).res.srcDirs.first()
                var stringsXmlFile = File(buildTypeSource, "values/strings.xml")
                if (!stringsXmlFile.exists()) {
                    buildTypeSource = android.sourceSets.getByName("main").res.srcDirs.first()
                    stringsXmlFile = File(buildTypeSource, "values/strings.xml")
                }

                val xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(stringsXmlFile)
                val xPath = XPathFactory.newInstance().newXPath()
                val expression = "/resources/string[@name='app_name']/text()"
                val appName = xPath.evaluate(expression, xmlDocument, XPathConstants.STRING) as String

                val output = """
                    APPLICATION_ID=${variant.applicationId}
                    APPLICATION_LABEL=$appName
                    VERSION_CODE=${flavor.versionCode}
                    VERSION_NAME=${flavor.versionName}
                    VERSION_NAME_SUFFIX=${flavor.versionNameSuffix ?: ""}
                    FULL_VERSION_NAME=${flavor.versionName}${flavor.versionNameSuffix ?: ""}
                """.trimIndent()

                println(output)
                val githubOutput = System.getenv("GITHUB_OUTPUT")
                if (githubOutput != null) {
                    val outputFile = File(githubOutput)
                    outputFile.writeText(output + "\n")
                }
            }
            true
        }
    }
}
