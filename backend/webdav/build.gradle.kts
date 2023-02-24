@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.backend.api)
    api(projects.mail.protocols.webdav)

    testImplementation(projects.mail.testing)
}
