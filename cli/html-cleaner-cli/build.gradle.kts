plugins {
    id(ThunderbirdPlugins.App.jvm)
}

version = "unspecified"

application {
    mainClass.set("app.k9mail.cli.html.cleaner.MainKt")
}

dependencies {
    implementation(projects.library.htmlCleaner)

    implementation(libs.clikt)
    implementation(libs.okio)
}
