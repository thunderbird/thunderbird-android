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

    implementation(libs.clikt)
    implementation(libs.kxml2)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}

tasks.named<Sync>("installDist") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.named<Zip>("distZip") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
tasks.named<Tar>("distTar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
