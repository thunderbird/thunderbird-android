plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.account.settings"
    resourcePrefix = "account_settings_"
}

dependencies {
    api(projects.feature.account.settings.api)

    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.compose.navigation)

    testImplementation(projects.core.ui.compose.testing)
}
