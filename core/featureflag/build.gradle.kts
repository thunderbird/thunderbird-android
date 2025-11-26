plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.featureflag"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.annotation)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}
