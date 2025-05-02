plugins {
    id(ThunderbirdPlugins.App.jvm)
    alias(libs.plugins.kotlin.serialization)
}

version = "unspecified"

application {
    mainClass.set("net.thunderbird.cli.translation.MainKt")
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.serialization.json)
    implementation(libs.logback.classic)
}
