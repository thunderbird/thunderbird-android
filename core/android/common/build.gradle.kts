plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.core.android.common"
}

dependencies {
    api(projects.core.common)


    implementation(libs.androidx.webkit)

    implementation(libs.core.ktx)
    implementation(projects.core.ui.legacy.theme2.k9mail)
    testImplementation(projects.core.testing)
    testImplementation(libs.robolectric)
    testImplementation(projects.legacy.core)
}
