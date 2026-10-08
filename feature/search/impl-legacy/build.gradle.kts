plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.search.legacy"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.mail.account.api)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
