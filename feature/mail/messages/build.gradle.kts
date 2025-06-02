plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.mail.messages"
}

dependencies {
    implementation(libs.androidx.compose.material3)

    implementation(projects.backend.api)
    implementation(projects.core.android.common)
    implementation(projects.core.logging.api)
    implementation(projects.core.outcome)
    implementation(projects.core.preferences)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.theme.api)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
    implementation(projects.legacy.mailstore)
    implementation(projects.mail.common)

    testImplementation(libs.mockk)
}
