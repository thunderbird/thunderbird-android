plugins {
    id("thunderbird.app.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("thunderbird.quality.detekt.typed")
    id("thunderbird.quality.kover")
    id("thunderbird.quality.spotless")
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

    implementation(libs.androidx.activity.compose)
}
