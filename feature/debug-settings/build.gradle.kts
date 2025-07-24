plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.debug.settings"
    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.compose.navigation)
    implementation(projects.core.common)
    implementation(projects.core.outcome)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.notification.api)
}
