plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.ui.contract"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.common)
        }

        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
