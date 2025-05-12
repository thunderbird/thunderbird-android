plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(projects.core.account)
    implementation(projects.mail.common)

    testImplementation(projects.core.testing)
}
