plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.outcome)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
    api(projects.mail.common)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
