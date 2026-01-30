plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.legacy.ui.legacy)
    implementation(projects.legacy.core)
}

android {
    namespace = "app.k9mail.feature.widget.message.list"
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
