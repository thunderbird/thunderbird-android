plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.logging.testing"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.logging.api)
        }
    }
}

codeCoverage {
    lineCoverage.set(0)
}
