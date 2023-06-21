plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.app.core)

    api(projects.core.android.testing)

    api(libs.koin.core)
    api(libs.mockito.core)
    api(libs.mockito.kotlin)
}

android {
    namespace = "com.fsck.k9.testing"
}
