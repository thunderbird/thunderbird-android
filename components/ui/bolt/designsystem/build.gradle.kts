plugins {
    id("thunderbird.library.kmp.compose")
}

kotlin {
    android {
        namespace = "app.k9mail.core.ui.compose.designsystem"
        androidResources.enable = true
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.ui.bolt.theme)

            implementation(libs.jetbrains.compose.material3)
            implementation(libs.jetbrains.compose.material3.adaptive)
            implementation(libs.jetbrains.compose.material3.adaptive.layout)
            implementation(libs.jetbrains.compose.material3.adaptive.navigation)
            implementation(libs.jetbrains.compose.material.icons.extended)
            implementation(libs.jetbrains.compose.navigation.event)

            implementationWithExcludes(libs.landscapist.coil) {
                exclude(group = "io.coil-kt.coil3", module = "coil-network-ktor3")
                exclude(group = "io.ktor")
                exclude(group = "org.jetbrains.skiko")
            }
        }

        commonTest.dependencies {
            implementation(projects.ui.testing)
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
