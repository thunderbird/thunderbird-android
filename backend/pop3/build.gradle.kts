plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.backend.api)
    api(projects.mail.protocols.pop3)
    api(projects.mail.protocols.smtp)

    testImplementation(projects.mail.testing)
}
