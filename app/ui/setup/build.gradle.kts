plugins {
    id "thunderbird.library.android"
}

dependencies {
    api project(":app:ui:base")
    implementation project(":app:core")
    implementation project(":app:autodiscovery:api")
    implementation project(":mail:common")

    implementation libs.androidx.lifecycle.viewmodel.ktx
    implementation libs.androidx.lifecycle.livedata.ktx
    implementation libs.androidx.constraintlayout
    implementation libs.androidx.core.ktx
    implementation libs.timber
    implementation libs.kotlinx.coroutines.core
    implementation libs.kotlinx.coroutines.android

    testImplementation project(':mail:testing')
    testImplementation project(':app:testing')
    testImplementation libs.robolectric
}

android {
    namespace 'com.fsck.k9.ui.setup'
}
