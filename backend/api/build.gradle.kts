plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(projects.core.account)
    implementation(projects.core.outcome)
    api(projects.mail.common)
}
