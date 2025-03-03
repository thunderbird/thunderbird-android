plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.navigation.drawer"
    resourcePrefix = "navigation_drawer_"
}

dependencies {
    implementation(projects.core.mail.folder.api)

    implementation(projects.core.ui.theme.api)
    implementation(projects.core.ui.compose.designsystem)

    implementation(projects.feature.account.avatar)

    implementation(projects.legacy.account)
    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)
    implementation(projects.legacy.search)
    implementation(projects.legacy.ui.folder)

    testImplementation(projects.core.ui.compose.testing)
}
