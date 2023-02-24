plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(projects.app.ui.base)
    implementation(projects.app.core)
    implementation(projects.app.autodiscovery.api)
    implementation(projects.mail.common)

    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.mail.testing)
    testImplementation(projects.app.testing)
    testImplementation(libs.robolectric)
}

android {
    namespace = "com.fsck.k9.ui.setup"
}
