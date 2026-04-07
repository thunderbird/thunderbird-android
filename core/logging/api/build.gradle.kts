plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.logging"
        withHostTest {}
    }
    sourceSets {
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}

codeCoverage {
    lineCoverage = 68
}
