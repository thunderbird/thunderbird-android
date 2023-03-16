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

    implementation(libs.bundles.shared.jvm.android.compose)

    debugImplementation(libs.bundles.shared.jvm.android.compose.debug)

    testImplementation(libs.bundles.shared.jvm.test.compose)

    androidTestImplementation(libs.bundles.shared.jvm.androidtest.compose)
}
