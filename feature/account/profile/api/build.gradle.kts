plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.account.profile"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.architecture.api)
            api(projects.feature.account.api)
            api(projects.feature.account.avatar.api)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
