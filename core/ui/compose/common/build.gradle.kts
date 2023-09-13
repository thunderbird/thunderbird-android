plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.common"
    resourcePrefix = "core_ui_common_"
}

dependencies {
    implementation(libs.androidx.compose.activity)

    testImplementation(projects.core.ui.compose.testing)
}
