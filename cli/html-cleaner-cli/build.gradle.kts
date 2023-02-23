@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.kotlin.jvm)
    id("application")
}

version = "unspecified"

application {
    mainClass.set("app.k9mail.cli.html.cleaner.MainKt")
}

dependencies {
    implementation(projects.app.htmlCleaner)

    implementation(libs.clikt)
    implementation(libs.okio)
}

java {
    sourceCompatibility = ThunderbirdProjectConfig.javaVersion
    targetCompatibility = ThunderbirdProjectConfig.javaVersion
}
