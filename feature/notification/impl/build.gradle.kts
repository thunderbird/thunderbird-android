plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
    alias(libs.plugins.dev.mokkery)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.notification"
        withHostTest {
            isIncludeAndroidResources = true
        }
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
            implementation(libs.androidx.test.core)
            implementation(libs.mockito.core)
            implementation(libs.mockito.kotlin)
            implementation(libs.robolectric)
        }
    }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.notification.resources.impl"
}

codeCoverage {
    branchCoverage.set(29)
    lineCoverage.set(31)
}
