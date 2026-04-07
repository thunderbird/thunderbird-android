plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.logging.legacy"
    }
    sourceSets {
        androidMain.dependencies {
            implementation(libs.timber)
            implementation(projects.core.logging.implComposite)
            implementation(projects.core.logging.implFile)
            implementation(projects.core.logging.config)
        }

        commonMain.dependencies {
            api(projects.core.logging.api)
        }

        commonTest.dependencies {
            implementation(projects.core.logging.testing)
        }
    }
}

codeCoverage {
    branchCoverage = 30
}
