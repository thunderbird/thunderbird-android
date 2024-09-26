plugins {
    // TODO: Change to ThunderbirdPlugins.Library.androidCompose when integrating the feature into the app.
    id(ThunderbirdPlugins.App.androidCompose)
}

android {
    namespace = "app.k9mail.feature.migration.qrcode"
    resourcePrefix = "migration_qrcode_"
}

dependencies {
    implementation(projects.core.common)

    implementation(projects.core.ui.compose.designsystem)
    debugImplementation(projects.core.ui.compose.theme2.k9mail)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.moshi)
    implementation(libs.timber)

    testImplementation(projects.core.ui.compose.testing)
    testImplementation(projects.core.ui.compose.theme2.k9mail)
}
