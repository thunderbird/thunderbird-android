plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
    alias(libs.plugins.dev.mokkery)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.thundermail"
        androidResources.enable = true
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.featureflag)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.thundermail.resources"
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
