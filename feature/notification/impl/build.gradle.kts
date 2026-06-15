plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.notification"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.featureflag)
            implementation(projects.core.outcome)
            implementation(projects.core.logging.api)
            implementation(projects.feature.notification.api)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(projects.core.logging.testing)
            implementation(projects.feature.notification.testing)
        }
        androidMain.dependencies {
            // should split feature.launcher into api/impl?
            implementation(projects.feature.launcher)
            implementation(projects.core.ui.theme.api)
        }
        androidHostTest.dependencies {
            implementation(libs.mockito.core)
            implementation(libs.mockito.kotlin)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.notification.resources.impl"
}

codeCoverage {
    branchCoverage = 29
    lineCoverage = 31
}
