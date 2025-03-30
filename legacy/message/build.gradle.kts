plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.message"
}

dependencies {
    implementation(projects.legacy.account)
    implementation(projects.legacy.search)
    implementation(projects.feature.search)

    implementation(projects.mail.common)
    implementation(projects.backend.api)
}
