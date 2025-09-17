plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.account"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.architecture.api)
        }
    }
}

codeCoverage {
    lineCoverage.set(8)
}
