plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.account.settings"
    resourcePrefix = "account_settings_"
}

dependencies {
    api(projects.feature.account.settings.api)
    implementation(projects.feature.account.core)
    implementation(projects.feature.account.avatar.impl)

    implementation(projects.core.outcome)

    implementation(projects.core.logging.implLegacy)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.compose.navigation)
    implementation(projects.core.ui.compose.preference)
    implementation(projects.core.ui.legacy.theme2.common)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.core.ui.compose.testing)
}
