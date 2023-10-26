plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.onboarding.permissions"
    resourcePrefix = "onboarding_permissions_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.android.permissions)
    implementation(projects.feature.account.common)
}
