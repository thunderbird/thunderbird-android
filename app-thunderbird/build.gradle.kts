plugins {
    id(ThunderbirdPlugins.App.androidCompose)
    alias(libs.plugins.dependency.guard)
    id("thunderbird.quality.badging")
}

val testCoverageEnabled: Boolean by extra
if (testCoverageEnabled) {
    apply(plugin = "jacoco")
}

android {
    namespace = "net.thunderbird.android"

    defaultConfig {
        applicationId = "net.thunderbird.android"
        testApplicationId = "net.thunderbird.android.tests"

        versionCode = 2
        versionName = "0.1"

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
        createSigningConfig(project, SigningType.TB_RELEASE)
        createSigningConfig(project, SigningType.TB_BETA)
        createSigningConfig(project, SigningType.TB_DAILY)
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-SNAPSHOT"

            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true

            buildConfigField("String", "RELEASE_CHANNEL", "null")
        }

        release {
            signingConfig = signingConfigs.getByType(SigningType.TB_RELEASE)

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )

            buildConfigField("String", "RELEASE_CHANNEL", "\"release\"")
        }

        create("beta") {
            signingConfig = signingConfigs.getByType(SigningType.TB_BETA)

            applicationIdSuffix = ".beta"
            versionNameSuffix = "b1"

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            matchingFallbacks += listOf("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )

            buildConfigField("String", "RELEASE_CHANNEL", "\"beta\"")
        }

        create("daily") {
            signingConfig = signingConfigs.getByType(SigningType.TB_DAILY)

            applicationIdSuffix = ".daily"
            versionNameSuffix = "a1"

            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false

            matchingFallbacks += listOf("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro",
            )

            buildConfigField("String", "RELEASE_CHANNEL", "\"daily\"")
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

dependencies {
    implementation(projects.appCommon)
    implementation(projects.core.ui.compose.theme2.thunderbird)
    implementation(projects.core.ui.legacy.theme2.thunderbird)
    implementation(projects.feature.launcher)

    implementation(projects.legacy.core)
    implementation(projects.legacy.ui.legacy)

    implementation(projects.core.featureflags)

    implementation(projects.feature.widget.messageList)
    implementation(projects.feature.widget.shortcut)
    implementation(projects.feature.widget.unread)

    debugImplementation(projects.feature.telemetry.noop)
    releaseImplementation(projects.feature.telemetry.glean)
    "betaImplementation"(projects.feature.telemetry.glean)
    "dailyImplementation"(projects.feature.telemetry.glean)

    implementation(libs.androidx.work.runtime)

    implementation(projects.feature.autodiscovery.api)
    debugImplementation(projects.backend.demo)
    debugImplementation(projects.feature.autodiscovery.demo)

    testImplementation(libs.robolectric)

    // Required for DependencyInjectionTest to be able to resolve OpenPgpApiManager
    testImplementation(projects.plugins.openpgpApiLib.openpgpApi)
    testImplementation(projects.feature.account.setup)
}

dependencyGuard {
    configuration("releaseRuntimeClasspath")
}
