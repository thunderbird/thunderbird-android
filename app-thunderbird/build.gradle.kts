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
    implementation(projects.core.ui.compose.theme2.thunderbird)
    implementation(projects.core.ui.legacy.theme2.thunderbird)
    implementation(projects.feature.launcher)

    implementation(projects.app.core)
    implementation(projects.app.ui.legacy)

    implementation(projects.feature.widget.messageList)
    implementation(projects.feature.widget.shortcut)
    implementation(projects.feature.widget.unread)

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
    namespace = "net.thunderbird.android"

    defaultConfig {
        applicationId = "net.thunderbird.placeholder"
        testApplicationId = "net.thunderbird.placeholder.tests"

        versionCode = 1
        versionName = "0.1-SNAPSHOT"

        // Keep in sync with the resource string array "supported_languages"
        resourceConfigurations.addAll(
            listOf(
                "ar",
                "be",
                "bg",
                "br",
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
                "gd",
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
                "ml",
                "nb",
                "nl",
                "pl",
                "pt_BR",
                "pt_PT",
                "ro",
                "ru",
                "sk",
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

        buildConfigField("String", "CLIENT_INFO_APP_NAME", "\"Thunderbird\"")
    }

    signingConfigs {
        if (project.hasProperty("thunderbird.keyAlias") &&
            project.hasProperty("thunderbird.keyPassword") &&
            project.hasProperty("thunderbird.storeFile") &&
            project.hasProperty("thunderbird.storePassword")
        ) {
            create("release") {
                keyAlias = project.property("thunderbird.keyAlias") as String
                keyPassword = project.property("thunderbird.keyPassword") as String
                storeFile = file(project.property("thunderbird.storeFile") as String)
                storePassword = project.property("thunderbird.storePassword") as String
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }

        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
        }

        create("daily") {
            initWith(getByName("release"))
            applicationIdSuffix = ".daily"
            matchingFallbacks += listOf("release")
        }

        create("beta") {
            initWith(getByName("release"))
            applicationIdSuffix = ".beta"
            matchingFallbacks += listOf("release")
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
