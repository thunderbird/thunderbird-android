plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.notification.testing"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.outcome)
            api(projects.feature.notification.api)
        }
    }
}

codeCoverage {
    lineCoverage.set(0)
}
