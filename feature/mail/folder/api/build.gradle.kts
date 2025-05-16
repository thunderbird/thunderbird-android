plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.feature.mail.folder.api"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.mail.account.api)
        }
    }
}
