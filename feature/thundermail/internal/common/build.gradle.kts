plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.thundermail.internal.common"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.core.logging.api)
    implementation(projects.feature.thundermail.api)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
