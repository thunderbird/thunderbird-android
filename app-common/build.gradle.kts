plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.app.common"
}

dependencies {
    api(projects.legacy.common)

    implementation(projects.feature.account.core)

    implementation(projects.legacy.core)
    implementation(projects.legacy.account)

    implementation(projects.core.featureflags)

    implementation(projects.feature.migration.provider)
}
