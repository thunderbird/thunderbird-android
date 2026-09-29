plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.tb.piisafe)
}

kotlin {
    explicitApi()

    android {
        namespace = "net.thunderbird.feature.mail.storage.globaldb.migration"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.api)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
