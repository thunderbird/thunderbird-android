plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.account.server.settings"
    resourcePrefix = "account_server_settings_"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.common)
    implementation(projects.core.validation)

    implementation(projects.mail.common)
    implementation(projects.mail.protocols.imap)

    implementation(projects.feature.account.common)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.biometric)

    testImplementation(projects.core.ui.compose.testing)
}

codeCoverage {
    branchCoverage.set(44)
    lineCoverage.set(56)
}
