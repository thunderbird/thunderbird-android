plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.account.servercertificate"
    resourcePrefix = "account_servercertificate_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.common)
    implementation(projects.feature.account.common)

    implementation(projects.mail.common)

    testImplementation(projects.core.ui.compose.testing)
}
