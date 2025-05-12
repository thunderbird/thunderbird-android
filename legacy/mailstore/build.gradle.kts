plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.mailstore"
}

dependencies {
    implementation(projects.feature.search)
    implementation(projects.core.account)
    implementation(projects.legacy.account)
    implementation(projects.legacy.di)
    implementation(projects.legacy.message)

    implementation(projects.mail.common)
    implementation(projects.core.mail.folder.api)
    implementation(projects.feature.folder.api)
}
