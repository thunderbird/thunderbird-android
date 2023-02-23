plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "org.openintents.openpgp"

    buildFeatures {
        aidl = true
    }
}

dependencies {
    implementation(libs.androidx.lifecycle.common)
    implementation(libs.timber)
    implementation(libs.preferencex)
}
