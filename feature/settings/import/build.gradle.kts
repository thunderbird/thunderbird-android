plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    // Using "importing" because "import" is not allowed in Java package names (it's fine with Kotlin, though)
    namespace = "app.k9mail.feature.settings.importing"
    resourcePrefix = "settings_import_"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.legacy.core)
    implementation(projects.legacy.ui.base)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.feature.account.common)
    implementation(projects.feature.migration.launcher.api)
    implementation(projects.feature.account.oauth)
    implementation(projects.feature.thundermail.api)
    implementation(libs.appauth)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.fastadapter)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.core.ui.compose.testing)
}

codeCoverage {
    branchCoverage = 3
    lineCoverage = 4
}
