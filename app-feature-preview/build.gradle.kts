plugins {
    id(ThunderbirdPlugins.App.androidCompose)
}

android {
    namespace = "app.k9mail.feature.preview"

    defaultConfig {
        applicationId = "net.thunderbird.feature.preview"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)

    implementation(projects.feature.onboarding)
}
