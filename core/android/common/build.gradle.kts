plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.core.android.common"
}

dependencies {
    api(projects.core.common)

    implementation(libs.androidx.webkit)

    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
}
