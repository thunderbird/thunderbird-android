plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(projects.legacy.common)
    implementation(projects.feature.migration.provider)
}

android {
    namespace = "app.k9mail.common"
}
