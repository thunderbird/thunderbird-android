plugins {
    id(ThunderbirdPlugins.Library.jvm)
}

description = "PII logging K2 compiler plugin: FIR errors, toStringPiiSafe() generation, and " +
    "Logger call-site rewriting for net.thunderbird.core.logging.LoggingPii."

kotlin {
    explicitApi()
}

dependencies {
    implementation(projects.library.piiSafe.annotations)
    compileOnly(libs.kotlin.compiler)
    compileOnly(projects.core.logging.api)

    testImplementation(libs.kotlin.compiler)
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(projects.core.logging.api)
}
