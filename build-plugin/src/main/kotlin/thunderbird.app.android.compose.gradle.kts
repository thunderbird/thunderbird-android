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
    configureSharedComposeDependencies(libs)

    implementation(libs.androidx.compose.activity)
}
