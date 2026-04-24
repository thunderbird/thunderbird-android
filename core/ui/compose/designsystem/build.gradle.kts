plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "app.k9mail.core.ui.compose.designsystem"
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.ui.contract)
            api(projects.core.ui.common)
            api(projects.core.ui.compose.theme2)

            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.material3.adaptive)
            implementation(libs.jetbrains.compose.material3.adaptive.layout)
            implementation(libs.jetbrains.compose.material3.adaptive.navigation)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.jetbrains.compose.navigation.event)

            implementationWithExcludes(libs.landscapist.coil) {
                exclude(group = "io.coil-kt.coil3", module = "coil-network-ktor3")
                exclude(group = "io.ktor")
            }
        }

        commonTest.dependencies {
            implementation(projects.core.ui.testing)
        }

        androidMain.dependencies {
            implementation(libs.androidx.autofill)
        }
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "net.thunderbird.core.ui.designsystem.resources"
    generateResClass = always
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
