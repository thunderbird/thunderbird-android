plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.mail.message.composer.internal"
}

dependencies {
    implementation(projects.feature.mail.message.composer.api)

    implementation(libs.ksoup)
    implementation(projects.core.common)
    implementation(projects.core.ui.theme.api)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
