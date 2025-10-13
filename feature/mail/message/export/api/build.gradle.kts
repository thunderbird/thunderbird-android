plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.uri)
            implementation(projects.core.outcome)
        }
    }
}

android {
    namespace = "net.thunderbird.feature.mail.message.export"
}
