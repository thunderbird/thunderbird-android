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

    implementation(libs.commons.io)
    compileOnly(libs.apache.httpclient)

    testImplementation(projects.mail.testing)
    testImplementation(libs.apache.httpclient)
}
