plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.navigation.drawer"
    resourcePrefix = "navigation_drawer_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)

    implementation(projects.legacy.core)
    implementation(projects.legacy.ui.base)
    implementation(projects.legacy.ui.account)
    implementation(projects.legacy.ui.folder)
    implementation(projects.core.ui.legacy.designsystem)

    implementation(libs.materialdrawer)
    implementation(libs.androidx.swiperefreshlayout)

    testImplementation(projects.core.ui.compose.testing)
}
