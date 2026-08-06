plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.legacy.core)
    implementation(projects.legacy.logging)
    implementation(projects.legacy.ui.legacy)
}

android {
    namespace = "app.k9mail.feature.widget.message.list"
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
