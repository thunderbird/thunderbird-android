plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.mail.account.fake"
    }
    sourceSets {
        commonMain.dependencies {
            api(projects.feature.mail.account.api)
            implementation(libs.kotlinx.coroutines.core)
        }
    }
}

codeCoverage {
    lineCoverage = 0
}
