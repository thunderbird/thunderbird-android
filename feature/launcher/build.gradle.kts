plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.launcher"
    resourcePrefix = "launcher_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.legacy.ui.base)
    implementation(projects.feature.onboarding.main)
    implementation(projects.feature.settings.import)
    implementation(projects.feature.account.setup)
    implementation(projects.feature.account.edit)
    implementation(project(":feature:dddonboarding"))

    testImplementation(projects.core.ui.compose.testing)
}
