plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.library.baselineprofile.common"
}

dependencies {
    api(libs.androidx.benchmark.macro.junit4)
    api(libs.androidx.test.ext.junit.ktx)
    api(libs.androidx.test.runner)
    api(libs.androidx.test.uiautomator)
}
