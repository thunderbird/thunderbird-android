plugins {
    id(ThunderbirdPlugins.App.jvm)
}

version = "unspecified"

application {
    mainClass.set("net.thunderbird.cli.translation.MainKt")
}

dependencies {
    implementation(libs.clikt)
    implementation(platform(libs.http4k.bom))
    implementation(libs.http4k.core)
    implementation(libs.http4k.client.okhttp)
    implementation(libs.http4k.format.moshi)
}
