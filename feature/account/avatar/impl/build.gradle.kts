plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.account.avatar.impl"
    resourcePrefix = "account_avatar_"
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.common)
    implementation(projects.core.file)

    implementation(projects.feature.account.avatar.api)

    testImplementation(projects.core.ui.compose.testing)
}
