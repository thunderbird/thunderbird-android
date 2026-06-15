plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
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
    implementation(projects.core.ui.theme.api)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
    implementation(projects.feature.notification.api)
    implementation(projects.legacy.mailstore)
    implementation(projects.mail.common)

    testImplementation(projects.core.logging.testing)
    testImplementation(projects.core.ui.compose.testing)
    testImplementation(projects.feature.notification.testing)

    testImplementation(libs.mockito.kotlin)
}

codeCoverage {
    branchCoverage = 4
    lineCoverage = 9
}
