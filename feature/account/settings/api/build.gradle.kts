plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.account.settings.api"
    resourcePrefix = "account_settings_api_"
}

dependencies {
    implementation(projects.core.ui.compose.navigation)
}
