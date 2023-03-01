plugins {
    id("thunderbird.library.android")
}

android {
    configureSharedComposeConfig(libs)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:${libs.versions.androidxComposeBom.get()}")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.foundation:foundation")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // UI Tests
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    implementation(libs.androidx.compose.lifecycle.viewmodel)
}
