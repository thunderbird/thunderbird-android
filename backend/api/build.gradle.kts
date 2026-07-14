plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.mail.common)

    implementation(projects.core.common)
    implementation(projects.core.outcome)

    implementation(projects.feature.account.api)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
