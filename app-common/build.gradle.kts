plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.app.common"
}

dependencies {
    api(projects.legacy.common)

    implementation(projects.legacy.account)

    implementation(projects.feature.migration.provider)
}
