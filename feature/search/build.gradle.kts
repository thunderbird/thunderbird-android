plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.thunderbird.feature.search"
}

dependencies {
    implementation(projects.core.account)
}
