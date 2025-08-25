plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.android.network"
}

dependencies {
    api(projects.core.common)

    implementation(projects.core.logging.api)
    implementation(projects.core.logging.implLegacy)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
}
