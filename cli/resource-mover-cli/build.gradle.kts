plugins {
    id(ThunderbirdPlugins.App.jvm)
}

version = "unspecified"

application {
    mainClass.set("net.thunderbird.cli.resource.mover.MainKt")
}

dependencies {
    implementation(libs.clikt)
}
