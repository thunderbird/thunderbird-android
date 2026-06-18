plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.account"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.architecture.api)
        }
    }
}

codeCoverage {
    lineCoverage = 8
}
