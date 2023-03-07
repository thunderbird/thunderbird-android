plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.core.android.common"
}

dependencies {
    api(projects.core.common)
    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
}
