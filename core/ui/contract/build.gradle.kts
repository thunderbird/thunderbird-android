plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    explicitApi()

    android {
        namespace = "net.thunderbird.core.ui.contract"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
        }

        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(projects.core.ui.testing)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
