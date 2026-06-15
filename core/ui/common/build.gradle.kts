plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    explicitApi()

    android {
        namespace = "net.thunderbird.core.ui.common"
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.tb.mobile.components.ui.testing)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
