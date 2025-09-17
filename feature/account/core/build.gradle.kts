plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.account.core"
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.api)
            api(projects.feature.account.profile.api)
        }
    }
}

codeCoverage {
    lineCoverage.set(0)
}
