plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.onboarding"
    resourcePrefix = "onboarding_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
}
