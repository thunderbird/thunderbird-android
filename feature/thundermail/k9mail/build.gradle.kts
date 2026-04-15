plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
    alias(libs.plugins.dev.mokkery)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.thundermail.k9mail"
        androidResources.enable = true
        withHostTest {
            isIncludeAndroidResources = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.thundermail.api)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.thundermail.k9mail.resources"
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
