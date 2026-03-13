plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.account.avatar"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.api)
            api(libs.uri)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
