plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
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

codeCoverage {
    lineCoverage = 60
}
