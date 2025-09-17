plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.logging.file"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
            implementation(projects.core.logging.api)
        }
    }
}

codeCoverage {
    branchCoverage.set(50)
    lineCoverage.set(71)
}
