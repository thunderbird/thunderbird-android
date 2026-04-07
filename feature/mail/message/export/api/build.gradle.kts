plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.mail.message.export"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.uri)
            implementation(projects.core.outcome)
        }
    }
}
