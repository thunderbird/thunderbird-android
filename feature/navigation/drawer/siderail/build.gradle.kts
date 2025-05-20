plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.navigation.drawer.siderail"
    resourcePrefix = "navigation_drawer_siderail_"
}

dependencies {
    api(projects.feature.navigation.drawer.api)

    implementation(projects.core.mail.folder.api)

    implementation(projects.core.account)
    implementation(projects.core.ui.theme.api)
    implementation(projects.core.ui.compose.designsystem)

    implementation(projects.feature.account.avatar)

    implementation(projects.core.android.account)
    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)
    implementation(projects.feature.search)
    implementation(projects.legacy.ui.folder)
    implementation(projects.core.featureflags)

    testImplementation(projects.core.ui.compose.testing)
}
