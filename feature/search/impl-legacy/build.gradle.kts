plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "net.thunderbird.feature.search.legacy"
}

dependencies {
    implementation(projects.feature.mail.account.api)
    implementation(libs.kotlinx.serialization.json)
}
