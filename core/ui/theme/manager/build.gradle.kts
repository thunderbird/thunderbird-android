plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.ui.theme.manager"
}

dependencies {
    api(projects.core.ui.theme.api)

    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.core.preference.api)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
