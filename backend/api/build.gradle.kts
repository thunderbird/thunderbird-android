plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(projects.core.outcome)
    implementation(projects.core.mail.folder.api)
    implementation(projects.feature.mail.account.api)
    api(projects.mail.common)
}
