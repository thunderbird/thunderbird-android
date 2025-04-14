plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.core.android.testing"
}

dependencies {
    api(libs.junit)
    api(libs.robolectric)

    implementation(projects.legacy.core)

    api(libs.koin.core)
    api(libs.mockito.core)
    api(libs.mockito.kotlin)
}
