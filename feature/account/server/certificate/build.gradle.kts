plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.account.server.certificate"
    resourcePrefix = "account_server_certificate_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.common)
    implementation(projects.feature.account.common)

    implementation(projects.mail.common)
    implementation(libs.okio)

    testImplementation(projects.core.ui.compose.testing)
}

codeCoverage {
    branchCoverage.set(11)
    lineCoverage.set(20)
}
