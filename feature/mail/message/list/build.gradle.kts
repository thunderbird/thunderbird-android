plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.dev.mokkery)
}

android {
    namespace = "net.thunderbird.feature.mail.message.list"
}

dependencies {
    implementation(projects.backend.api)
    implementation(projects.core.android.common)
    implementation(projects.core.logging.api)
    implementation(projects.core.outcome)
    implementation(projects.core.preference.api)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.theme.api)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
    implementation(projects.legacy.mailstore)
    implementation(projects.mail.common)
}

codeCoverage {
    branchCoverage.set(9)
    lineCoverage.set(18)
}
