plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.messages"
}

dependencies {
    implementation(libs.androidx.compose.material3)

    implementation(projects.backend.api)
    implementation(projects.core.account)
    implementation(projects.core.android.common)
    implementation(projects.core.mail.folder.api)
    implementation(projects.core.outcome)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.theme.api)
    implementation(projects.feature.folder.api)
    implementation(projects.legacy.mailstore)
    implementation(projects.mail.common)
}
