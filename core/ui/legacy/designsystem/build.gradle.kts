plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.core.ui.legacy.designsystem"
}

dependencies {
    api(projects.core.ui.legacy.theme2.common)
}

codeCoverage {
    lineCoverage.set(0)
}
