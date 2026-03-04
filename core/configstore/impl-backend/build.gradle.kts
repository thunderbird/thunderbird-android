plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.configstore.backend"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.configstore.api)
            implementation(projects.core.logging.api)

            implementation(libs.androidx.datastore.preferences)
        }

        commonTest.dependencies {
            implementation(projects.core.testing)
            implementation(libs.assertk)
        }

        jvmTest.dependencies {
            implementation(libs.junit)
        }
    }
}

codeCoverage {
    branchCoverage = 42
}
