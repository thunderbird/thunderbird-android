plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.funding.link"
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
