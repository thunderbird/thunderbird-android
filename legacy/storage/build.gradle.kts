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
    testImplementation(libs.robolectric)
    testImplementation(libs.commons.io)
}

android {
    namespace = "com.fsck.k9.storage"
}
