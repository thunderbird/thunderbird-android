plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(libs.koin.core)

    implementation(projects.legacy.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.mime4j.core)
    implementation(libs.commons.io)
    implementation(libs.moshi)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.mail.testing)
    testImplementation(projects.feature.telemetry.noop)
    testImplementation(libs.robolectric)
    testImplementation(libs.commons.io)
    testImplementation(projects.core.featureflag)
}

android {
    namespace = "com.fsck.k9.storage"
}
