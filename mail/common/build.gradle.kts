plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(libs.jetbrains.annotations)

    api(projects.core.logging.implLegacy)
    implementation(projects.core.common)

    implementation(libs.mime4j.core)
    implementation(libs.mime4j.dom)
    implementation(libs.okio)
    implementation(libs.commons.io)
    implementation(libs.moshi)

    // We're only using this for its DefaultHostnameVerifier
    implementation(libs.apache.httpclient5)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.mail.testing)
    testImplementation(libs.icu4j.charset)
}

codeCoverage {
    branchCoverage.set(27)
    lineCoverage.set(47)
}
