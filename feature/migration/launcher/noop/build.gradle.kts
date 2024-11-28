plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.feature.migration.launcher.noop"
}

dependencies {
    implementation(projects.feature.migration.launcher.api)
}
