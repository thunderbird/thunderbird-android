plugins {
    id(ThunderbirdPlugins.App.jvm)
}

version = "unspecified"

application {
    mainClass.set("app.k9mail.cli.autodiscovery.MainKt")
}

dependencies {
    implementation(projects.feature.autodiscovery.api)
    implementation(projects.feature.autodiscovery.autoconfig)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.clikt)
    implementation(libs.kxml2)
}

tasks.withType<Tar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Zip> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
