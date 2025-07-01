plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.thunderbird.feature.search.legacy"
}

dependencies {
    implementation(projects.feature.mail.account.api)
}
