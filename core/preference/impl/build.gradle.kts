plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.preference.impl"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.preference.api)

            implementation(projects.core.logging.api)
            implementation(projects.core.common)
        }
        commonTest.dependencies {
            implementation(projects.core.logging.testing)
        }
    }
}

codeCoverage {
    branchCoverage = 6
    lineCoverage = 2
}
