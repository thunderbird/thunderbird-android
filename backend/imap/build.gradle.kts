plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.backend.api)
    api(projects.core.outcome)

    api(projects.feature.mail.account.api)

    api(projects.mail.protocols.imap)
    api(projects.mail.protocols.smtp)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(projects.mail.testing)
    testImplementation(projects.backend.testing)
    testImplementation(libs.mime4j.dom)
}
