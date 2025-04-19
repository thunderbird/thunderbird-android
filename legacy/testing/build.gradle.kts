plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {

    api(projects.core.android.testing)

    api(libs.koin.core)
    api(libs.mockito.core)
    api(libs.mockito.kotlin)
}

android {
    namespace = "com.fsck.k9.testing"
}
