plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.preference.impl"
        withHostTest {}
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.preference.api)

            implementation(projects.core.logging.api)
            implementation(projects.core.common)
        }
    }
}

codeCoverage {
    branchCoverage = 6
    lineCoverage = 2
}
