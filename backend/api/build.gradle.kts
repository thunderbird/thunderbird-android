plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(projects.core.account)
    implementation(projects.core.outcome)
    implementation(projects.core.mail.folder.api)
    api(projects.mail.common)
}
