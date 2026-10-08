plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.funding.noop"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.feature.funding.api)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
