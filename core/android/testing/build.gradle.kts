plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.android.testing"
}

dependencies {
    api(libs.junit)
    api(libs.robolectric)

    implementation(projects.core.logging.api)
    implementation(projects.core.preference.api)
    implementation(projects.core.preference.impl)

    api(libs.koin.core)
    api(libs.mockito.core)
    api(libs.mockito.kotlin)
}
