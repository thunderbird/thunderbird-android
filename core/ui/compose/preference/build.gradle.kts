plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.thunderbird.core.ui.compose.preference"
    resourcePrefix = "core_ui_preference_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)

    testImplementation(projects.core.ui.compose.testing)
}
