plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.onboarding"
    resourcePrefix = "onboarding_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
}
