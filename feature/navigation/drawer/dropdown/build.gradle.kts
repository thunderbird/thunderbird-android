plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.navigation.drawer.dropdown"
    resourcePrefix = "navigation_drawer_dropdown_"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    api(projects.feature.navigation.drawer.api)

    implementation(projects.core.android.account)
    implementation(projects.core.ui.theme.api)
    implementation(projects.core.ui.compose.designsystem)

    implementation(projects.feature.account.avatar.impl)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
    implementation(projects.feature.notification.api)

    implementation(projects.feature.search.implLegacy)
    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)
    implementation(projects.legacy.ui.folder)
    implementation(projects.core.featureflag)
    implementation(projects.core.ui.compose.common)

    testImplementation(projects.core.ui.compose.testing)
    testImplementation(projects.core.logging.testing)

    // Fakes
    debugImplementation(projects.feature.account.fake)
    testImplementation(projects.feature.account.fake)
}
