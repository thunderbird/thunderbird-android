plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.designsystem"
    resourcePrefix = "designsystem_"
}

dependencies {
    api(projects.core.ui.compose.theme2.common)

    debugApi(projects.core.ui.compose.theme2.k9mail)
    debugApi(projects.core.ui.compose.theme2.thunderbird)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(projects.core.ui.compose.testing)
}
