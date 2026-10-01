plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.funding.api"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core.ui.navigation)
            implementation(projects.core.configstore.api)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
