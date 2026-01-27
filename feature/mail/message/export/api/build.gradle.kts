plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.mail.message.export"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            implementation(libs.uri)
            implementation(projects.core.outcome)
        }
    }
}
