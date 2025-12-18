plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.di"

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.koin.android)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
