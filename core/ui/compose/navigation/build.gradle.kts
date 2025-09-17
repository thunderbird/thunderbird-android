plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.navigation"
    resourcePrefix = "core_ui_navigation_"
}

codeCoverage {
    lineCoverage.set(0)
}
