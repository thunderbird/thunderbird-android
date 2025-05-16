plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(projects.mail.common)

    implementation(projects.feature.mail.account.api)

    testImplementation(projects.core.testing)
}
