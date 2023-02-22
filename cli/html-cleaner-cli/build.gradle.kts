plugins {
    alias(libs.plugins.kotlin.jvm)
    id 'application'
}

version 'unspecified'

application {
    mainClass = "app.k9mail.cli.html.cleaner.MainKt"
}

dependencies {
    implementation project(':app:html-cleaner')

    implementation libs.clikt
    implementation libs.okio
}
