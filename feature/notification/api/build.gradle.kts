plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.notification.api"
        androidResources.enable = true
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.featureflag)
            implementation(projects.core.outcome)
        }
        commonTest.dependencies {
            implementation(projects.feature.notification.testing)
        }
        androidMain.dependencies {
            implementation(projects.core.ui.compose.common)
            implementation(projects.core.ui.compose.designsystem)
            implementation(projects.core.ui.compose.theme2)
        }
        androidHostTest.dependencies {
            implementation(projects.core.ui.compose.testing)
        }
        jvmTest.dependencies {
            implementation(libs.mockito.kotlin)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.notification.resources.api"
}

codeCoverage {
    branchCoverage = 46
    lineCoverage = 23
}
