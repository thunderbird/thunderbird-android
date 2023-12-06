plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.onboarding.success"
    resourcePrefix = "onboarding_success_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
}
