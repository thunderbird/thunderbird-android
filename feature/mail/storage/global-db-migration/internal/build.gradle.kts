plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.tb.piisafe)
}

kotlin {
    explicitApi()

    android {
        namespace = "net.thunderbird.feature.mail.storage.globaldb.migration.internal"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.mail.storage.globalDbMigration.api)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
