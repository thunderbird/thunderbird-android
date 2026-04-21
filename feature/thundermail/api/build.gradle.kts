plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.thundermail"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.core.featureflag)
    implementation(projects.core.ui.compose.theme2.common)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
