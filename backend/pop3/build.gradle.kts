plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.backend.api)
    api(projects.mail.protocols.pop3)
    api(projects.mail.protocols.smtp)
    implementation(projects.core.common)
    implementation(projects.feature.mail.folder.api)

    testImplementation(projects.mail.testing)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
