plugins {
    id("thunderbird.app.android")
}

android {
    configureSharedComposeConfig(libs)

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:${libs.versions.androidxComposeBom.get()}")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.compose.material:material")

    // Android Studio Preview support
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // UI Tests
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")

    implementation(libs.androidx.compose.lifecycle.viewmodel)

    implementation(libs.androidx.compose.activity)
}
