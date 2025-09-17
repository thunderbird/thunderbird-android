plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.account.common"
    resourcePrefix = "account_common_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.common)

    implementation(projects.mail.common)

    testImplementation(projects.core.ui.compose.testing)
}

codeCoverage {
    branchCoverage.set(45)
    lineCoverage.set(42)
}
