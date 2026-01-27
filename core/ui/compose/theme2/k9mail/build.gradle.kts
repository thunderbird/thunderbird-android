plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.theme2.k9mail"
    resourcePrefix = "core_ui_theme2_k9mail"
}

dependencies {
    api(projects.core.ui.compose.theme2.common)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
