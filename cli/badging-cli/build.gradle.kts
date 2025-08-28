plugins {
    id(ThunderbirdPlugins.App.jvm)
}

version = "unspecified"

application {
    mainClass.set("net.thunderbird.cli.badging.MainKt")
}

dependencies {
    implementation(projects.core.logging.api)
    implementation(libs.clikt)
    implementation(libs.diff.utils)
}
