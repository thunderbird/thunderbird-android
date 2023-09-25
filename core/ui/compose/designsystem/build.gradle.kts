plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.designsystem"
    resourcePrefix = "designsystem_"
}

dependencies {
    api(projects.core.ui.compose.theme.material2)
    implementation(libs.androidx.compose.material)

    testImplementation(projects.core.ui.compose.testing)
}
