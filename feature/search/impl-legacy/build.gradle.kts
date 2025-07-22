plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.thunderbird.feature.search.legacy"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.mail.account.api)

            implementation(libs.kotlinx.serialization.json)
        }
    }
}
