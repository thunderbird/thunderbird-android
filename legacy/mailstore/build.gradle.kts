plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.mailstore"
}

dependencies {
    implementation(projects.legacy.account)
    implementation(projects.legacy.di)
    implementation(projects.legacy.folder)
    implementation(projects.legacy.message)
    implementation(projects.legacy.search)

    implementation(projects.mail.common)
    implementation(projects.core.mail.folder.api)
    implementation(projects.feature.folder.api)
}
