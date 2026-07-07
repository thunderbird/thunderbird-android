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
            implementation(projects.core.common)
            implementation(projects.core.ui.contract)
            implementation(projects.core.featureflag)
        }
        androidMain.dependencies {
            implementation(projects.core.ui.theme.api)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
