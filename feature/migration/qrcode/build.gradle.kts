plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.feature.migration.qrcode"
    resourcePrefix = "migration_qrcode_"
}

dependencies {
    implementation(projects.core.common)
    implementation(libs.moshi)
    implementation(libs.timber)
}
