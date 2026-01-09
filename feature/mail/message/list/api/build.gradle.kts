plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.mail.message.list"
}

dependencies {
    api(projects.core.outcome)

    implementation(projects.core.common)
    implementation(projects.core.featureflag)
    implementation(projects.core.logging.api)
    implementation(projects.core.preference.api)
    implementation(projects.core.ui.compose.common)
    implementation(projects.feature.account.api)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
}
