plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.app.core)

    api(libs.junit)
    api(libs.robolectric)
    api(libs.koin.core)
    api(libs.mockito.core)
    api(libs.mockito.kotlin)
}

android {
    namespace = "com.fsck.k9.testing"
}
