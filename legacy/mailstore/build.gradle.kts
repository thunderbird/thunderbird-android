plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.mailstore"
}

dependencies {
    implementation(projects.legacy.di)
    implementation(projects.legacy.message)

    implementation(projects.core.common)
    implementation(projects.core.android.account)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
    implementation(projects.feature.search.implLegacy)

    implementation(projects.mail.common)
}
