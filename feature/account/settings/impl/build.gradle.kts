plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.account.settings"
    resourcePrefix = "account_settings_"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    api(projects.feature.account.settings.api)
    implementation(projects.feature.account.core)
    implementation(projects.feature.account.avatar.impl)

    implementation(projects.core.outcome)

    implementation(projects.core.ui.setting.api)

    implementation(projects.core.logging.implLegacy)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.compose.navigation)
    implementation(projects.core.ui.legacy.theme2.common)

    debugImplementation(projects.core.ui.setting.implDialog)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.core.ui.compose.testing)
}
