plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.thunderbird.core.ui.setting.dialog"
    resourcePrefix = "core_ui_setting_dialog_"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.core.ui.setting.api)

    implementation(projects.core.ui.compose.designsystem)

    testImplementation(projects.core.ui.compose.testing)
}
