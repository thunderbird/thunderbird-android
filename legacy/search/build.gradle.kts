plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "app.k9mail.legacy.search"
}

dependencies {
    implementation(projects.legacy.account)
}
