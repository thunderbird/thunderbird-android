plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.settings.push"
    resourcePrefix = "settings_push_"
}

dependencies {
    implementation(projects.app.core)
    implementation(projects.app.ui.base)
    implementation(projects.core.ui.compose.designsystem)

    // We include this to be able to use radio buttons which aren't in the design library yet. Since we're in the
    // process of switching from Material 2 to Material 3 and we want to get rid of the "Push folders" setting
    // (see <https://github.com/thunderbird/thunderbird-android/issues/7761>), it's easier to temporarily add radio
    // buttons to this module instead of modifying the design library that is currently being worked on.
    // TODO: Remove the "Push folders" setting and with it the need to directly include this dependency.
    implementation(libs.androidx.compose.material)

    implementation(libs.androidx.preference)
    implementation(libs.timber)
}
