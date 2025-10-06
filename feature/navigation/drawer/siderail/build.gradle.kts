plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.navigation.drawer.siderail"
    resourcePrefix = "navigation_drawer_siderail_"
}

dependencies {
    api(projects.feature.navigation.drawer.api)

    implementation(projects.core.ui.theme.api)
    implementation(projects.core.ui.compose.designsystem)

    implementation(projects.feature.account.avatar.impl)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)

    implementation(projects.core.android.account)
    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)
    implementation(projects.feature.search.implLegacy)
    implementation(projects.legacy.ui.folder)
    implementation(projects.core.featureflag)

    testImplementation(projects.core.ui.compose.testing)

    // Fakes
    debugImplementation(projects.feature.account.fake)
    testImplementation(projects.feature.account.fake)
}
