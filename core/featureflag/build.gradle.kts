plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.featureflag"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.androidx.annotation)
        }
    }
}
