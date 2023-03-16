plugins {
    id("thunderbird.library.android")
}

android {
    configureSharedComposeConfig(libs)
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.foundation)

    // Android Studio Preview support
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // UI Tests
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    implementation(libs.androidx.compose.lifecycle.viewmodel)

    testImplementation(libs.bundles.shared.jvm.test.compose)
}
