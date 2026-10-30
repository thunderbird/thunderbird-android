plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.notification.testing"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.api)
            api(projects.feature.notification.api)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
