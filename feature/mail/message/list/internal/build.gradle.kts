plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.dev.mokkery)
}

android {
    namespace = "net.thunderbird.feature.mail.message.list.internal"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.feature.mail.message.list.api)

    implementation(projects.backend.api)
    implementation(projects.core.android.account)
    implementation(projects.core.android.common)
    implementation(projects.core.logging.api)
    implementation(projects.core.outcome)
    implementation(projects.core.preference.api)
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.theme.api)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
    implementation(projects.feature.notification.api)
    implementation(projects.legacy.mailstore)
    implementation(projects.mail.common)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.core.ui.compose.testing)
    testImplementation(libs.bundles.shared.jvm.test.compose)
    testImplementation(libs.bundles.shared.jvm.android.compose.debug)
    testImplementation(projects.feature.notification.testing)
}

codeCoverage {
    branchCoverage = 4
    lineCoverage = 9
}
