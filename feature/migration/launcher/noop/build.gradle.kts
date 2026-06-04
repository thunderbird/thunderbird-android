plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.feature.migration.launcher.k9"
}

dependencies {
    implementation(projects.feature.migration.launcher.api)
    implementation(projects.feature.migration.qrcode)
}

codeCoverage {
    lineCoverage = 0
}
