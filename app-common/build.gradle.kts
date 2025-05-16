plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.app.common"
}

dependencies {
    api(projects.legacy.common)
    api(projects.legacy.ui.legacy)

    api(projects.feature.account.core)
    api(projects.feature.launcher)
    api(projects.feature.navigation.drawer.api)

    implementation(projects.legacy.core)
    implementation(projects.core.android.account)

    implementation(projects.core.featureflag)
    implementation(projects.core.ui.legacy.theme2.common)

    implementation(projects.feature.account.setup)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.migration.provider)
    implementation(projects.feature.widget.messageList)

    implementation(projects.mail.protocols.imap)

    implementation(libs.androidx.work.runtime)
}
