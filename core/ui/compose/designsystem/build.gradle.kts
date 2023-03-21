plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.designsystem"
    resourcePrefix = "designsystem_"
}

dependencies {
    api(projects.core.ui.compose.theme)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(projects.core.ui.compose.testing)
}
