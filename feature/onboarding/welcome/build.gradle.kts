plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.onboarding.welcome"
    resourcePrefix = "onboarding_welcome_"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.ui.compose.designsystem)
}
