plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.message"
}

dependencies {
    implementation(projects.core.android.account)
    implementation(projects.feature.search.implLegacy)

    implementation(projects.mail.common)
    implementation(projects.backend.api)
}
