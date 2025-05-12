plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.navigation.drawer.dropdown"
    resourcePrefix = "navigation_drawer_dropdown_"
}

dependencies {
    api(projects.feature.navigation.drawer.api)

    implementation(projects.core.mail.folder.api)

    implementation(projects.core.ui.theme.api)
    implementation(projects.core.ui.compose.designsystem)

    implementation(projects.feature.account.avatar)

    implementation(projects.feature.search)
    implementation(projects.core.account)
    implementation(projects.legacy.account)
    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)
    implementation(projects.legacy.ui.folder)
    implementation(projects.core.featureflags)

    testImplementation(projects.core.ui.compose.testing)
}
