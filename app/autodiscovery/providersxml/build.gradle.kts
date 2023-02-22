plugins {
    id "thunderbird.library.android"
}

dependencies {
    implementation project(":app:core")
    implementation project(":mail:common")
    implementation project(":app:autodiscovery:api")

    implementation libs.timber

    testImplementation project(':app:testing')
    testImplementation project(":backend:imap")
    testImplementation libs.robolectric
    testImplementation libs.androidx.test.core
}

android {
    namespace 'com.fsck.k9.autodiscovery.providersxml'
}
