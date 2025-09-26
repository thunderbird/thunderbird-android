plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.feature.mail.folder.api"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.outcome)
            implementation(projects.feature.account.api)
            implementation(projects.feature.mail.account.api)
            implementation(libs.androidx.annotation)
        }
    }
}
