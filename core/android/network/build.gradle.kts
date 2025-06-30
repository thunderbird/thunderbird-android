plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.android.network"
}

dependencies {
    api(projects.core.common)

    implementation(libs.timber)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
}
