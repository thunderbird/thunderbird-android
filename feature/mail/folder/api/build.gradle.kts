plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.tb.piisafe)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.mail.folder.api"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.core.architecture.api)
            implementation(projects.core.common)
            implementation(projects.core.logging.api)
            implementation(projects.feature.account.api)
            implementation(projects.feature.mail.account.api)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
