plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.account.fake"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.api)
            api(projects.feature.account.profile.api)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
