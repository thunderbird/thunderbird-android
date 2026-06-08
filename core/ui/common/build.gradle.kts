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
            implementation(projects.core.ui.testing)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
