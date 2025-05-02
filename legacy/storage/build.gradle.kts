plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(libs.koin.core)

    implementation(projects.legacy.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.timber)
    implementation(libs.mime4j.core)
    implementation(libs.commons.io)
    implementation(libs.moshi)

    testImplementation(projects.mail.testing)
    testImplementation(projects.legacy.testing)
    testImplementation(projects.feature.telemetry.noop)
    testImplementation(libs.robolectric)
    testImplementation(libs.commons.io)
    testImplementation(projects.core.featureflags)
}

android {
    namespace = "com.fsck.k9.storage"
}
