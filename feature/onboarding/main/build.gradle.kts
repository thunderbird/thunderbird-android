plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.onboarding.main"
    resourcePrefix = "onboarding_main_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.feature.onboarding.welcome)
}
