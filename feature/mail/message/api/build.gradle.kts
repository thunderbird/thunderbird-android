plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.tb.piisafe)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.mail.message"
    }
    sourceSets {
        commonMain.dependencies {
            api(libs.uri)
            api(projects.core.architecture.api)
            implementation(projects.feature.account.api)
            api(projects.feature.mail.folder.api)
        }
    }
}
