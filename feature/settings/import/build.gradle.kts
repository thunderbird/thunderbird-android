plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    // Using "importing" because "import" is not allowed in Java package names (it's fine with Kotlin, though)
    namespace = "app.k9mail.feature.settings.importing"
    resourcePrefix = "settings_import_"
}

dependencies {
    implementation(projects.legacy.core)
    implementation(projects.legacy.ui.base)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.feature.migration.launcher.api)
    implementation(projects.feature.account.oauth)
    implementation(libs.appauth)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.fastadapter)

    implementation(libs.timber)
}
