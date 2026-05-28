plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "com.ciphermail.smime.api"

    buildFeatures {
        aidl = true
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
