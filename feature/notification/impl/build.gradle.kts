plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
    alias(libs.plugins.dev.mokkery)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.featureflag)
            implementation(projects.core.outcome)
            implementation(projects.core.logging.api)
            implementation(projects.feature.notification.api)
        }
        commonTest.dependencies {
            implementation(projects.core.logging.testing)
            implementation(projects.feature.notification.testing)
        }
        androidMain.dependencies {
            // should split feature.launcher into api/impl?
            implementation(projects.feature.launcher)
            implementation(projects.core.ui.theme.api)
        }
        androidUnitTest.dependencies {
            implementation(libs.androidx.test.core)
            implementation(libs.mockito.core)
            implementation(libs.mockito.kotlin)
            implementation(libs.robolectric)
        }
    }
}

android {
    namespace = "net.thunderbird.feature.notification"
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.notification.resources.impl"
}
