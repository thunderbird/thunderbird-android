plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.mail.folder.api"
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.outcome)
            implementation(projects.feature.account.api)
            implementation(projects.feature.mail.account.api)
            implementation(libs.androidx.annotation)
        }
    }
}
