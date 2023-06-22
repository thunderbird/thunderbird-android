plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.account.setup"
    resourcePrefix = "account_setup_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.common)

    implementation(projects.feature.autodiscovery.service)
    implementation(projects.feature.account.oauth)

    testImplementation(projects.core.ui.compose.testing)
}
