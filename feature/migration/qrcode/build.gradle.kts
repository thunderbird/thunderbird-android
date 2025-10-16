plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.migration.qrcode"
    resourcePrefix = "migration_qrcode_"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.android.account)
    implementation(projects.legacy.common)
    implementation(projects.legacy.ui.base)
    implementation(projects.core.ui.compose.designsystem)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.moshi)
    implementation(libs.okio)
    implementation(libs.zxing)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.core.ui.compose.testing)
    testImplementation(projects.core.ui.compose.theme2.k9mail)
}

codeCoverage {
    branchCoverage.set(58)
    lineCoverage.set(67)
}
