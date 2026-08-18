plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.mail.message.reader.api"
        androidResources {
            enable = true
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.tb.mobile.components.ui.bolt)
            implementation(projects.core.common)
            implementation(projects.core.ui.contract)
            implementation(projects.core.featureflag)
            implementation(projects.feature.account.api)

            implementation(libs.jetbrains.compose.ui.tooling)
        }
        androidMain.dependencies {
            implementation(projects.core.ui.theme.api)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
