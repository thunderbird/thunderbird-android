plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.feature.search.legacy"
        withHostTest {}
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.mail.account.api)

            implementation(libs.kotlinx.serialization.json)
        }
    }
}
