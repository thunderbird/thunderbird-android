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

        versionCode = 4
        versionName = "8.0"

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
        val useUploadKey = properties.getOrDefault("tb.useUploadKey", "true") == "true"

        createSigningConfig(project, SigningType.TB_RELEASE, isUpload = useUploadKey)
        createSigningConfig(project, SigningType.TB_BETA, isUpload = useUploadKey)
        createSigningConfig(project, SigningType.TB_DAILY, isUpload = useUploadKey)
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-SNAPSHOT"

            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true

            buildConfigField("String", "GLEAN_RELEASE_CHANNEL", "null")
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

            buildConfigField("String", "GLEAN_RELEASE_CHANNEL", "\"release\"")
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

            buildConfigField("String", "GLEAN_RELEASE_CHANNEL", "\"beta\"")
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

            // See https://bugzilla.mozilla.org/show_bug.cgi?id=1918151
            buildConfigField("String", "GLEAN_RELEASE_CHANNEL", "\"nightly\"")
        }
    }

    flavorDimensions += listOf("app")
    productFlavors {
        create("foss") {
            dimension = "app"
            buildConfigField("String", "PRODUCT_FLAVOR_APP", "\"foss\"")
        }

        create("full") {
            dimension = "app"
            buildConfigField("String", "PRODUCT_FLAVOR_APP", "\"full\"")
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

// Initialize placeholders for the product flavor and build type combinations needed for dependency declarations.
// They are required to avoid "Unresolved configuration" errors.
val fullDebugImplementation by configurations.creating
val fullDailyImplementation by configurations.creating
val fullBetaImplementation by configurations.creating
val fullReleaseImplementation by configurations.creating

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
    "dailyImplementation"(projects.feature.telemetry.noop)
    "betaImplementation"(projects.feature.telemetry.noop)
    releaseImplementation(projects.feature.telemetry.noop)

    implementation(libs.androidx.work.runtime)

    implementation(projects.feature.autodiscovery.api)
    debugImplementation(projects.backend.demo)
    debugImplementation(projects.feature.autodiscovery.demo)

    "fossImplementation"(projects.feature.funding.link)

    fullDebugImplementation(projects.feature.funding.googleplay)
    fullDailyImplementation(projects.feature.funding.googleplay)
    fullBetaImplementation(projects.feature.funding.googleplay)
    fullReleaseImplementation(projects.feature.funding.googleplay)

    implementation(projects.feature.onboarding.migration.thunderbird)

    testImplementation(libs.robolectric)

    // Required for DependencyInjectionTest to be able to resolve OpenPgpApiManager
    testImplementation(projects.plugins.openpgpApiLib.openpgpApi)
    testImplementation(projects.feature.account.setup)
}

dependencyGuard {
    configuration("fossDailyRuntimeClasspath")
    configuration("fossBetaRuntimeClasspath")
    configuration("fossReleaseRuntimeClasspath")

    configuration("fullDailyRuntimeClasspath")
    configuration("fullBetaRuntimeClasspath")
    configuration("fullReleaseRuntimeClasspath")
}

tasks.create("printConfigurations") {
    doLast {
        configurations.forEach { configuration ->
            println("Configuration: ${configuration.name}")
            configuration.dependencies.forEach { dependency ->
                println("  - ${dependency.group}:${dependency.name}:${dependency.version}")
            }
        }
    }
}
