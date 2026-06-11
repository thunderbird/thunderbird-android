plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "app.k9mail.core.ui.compose.designsystem"
    resourcePrefix = "designsystem_"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    api(projects.core.ui.contract)
    api(projects.core.ui.common)
    api(projects.core.ui.compose.theme2)

    implementation(libs.androidx.autofill)
    implementation(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.material3.adaptive)
    implementation(libs.jetbrains.compose.material3.adaptive.layout)
    implementation(libs.jetbrains.compose.material3.adaptive.navigation)
    implementation(libs.jetbrains.compose.material.icons.extended)
    implementation(libs.jetbrains.compose.navigation.event)

    // Landscapist imports a lot of dependencies that we don't need. We exclude them here.
    implementation(libs.landscapist.coil) {
        exclude(group = "io.coil-kt.coil3", module = "coil-network-ktor3")
        exclude(group = "io.ktor")
    }

    testImplementation(projects.core.ui.compose.testing)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
