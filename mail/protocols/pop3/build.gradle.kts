@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

val testCoverageEnabled: Boolean by extra
if (testCoverageEnabled) {
    apply(plugin = "jacoco")
}

dependencies {
    api(projects.mail.common)

    testImplementation(libs.bundles.shared.jvm.test.legacy)
    testImplementation(projects.mail.testing)
    testImplementation(libs.okio)
    testImplementation(libs.jzlib)
    testImplementation(libs.commons.io)
}
