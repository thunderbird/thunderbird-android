plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.thunderbird.core.ui.setting.component"
    resourcePrefix = "core_ui_setting_component_"
}

dependencies {
    implementation(projects.core.ui.setting.api)

    implementation(projects.core.ui.compose.designsystem)

    testImplementation(projects.core.ui.compose.testing)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
