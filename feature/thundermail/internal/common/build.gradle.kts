plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "net.thunderbird.feature.thundermail.internal.common"
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.appauth)

    implementation(projects.core.logging.api)
    implementation(projects.core.outcome)
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.contract)
    implementation(projects.core.ui.navigation)
    implementation(projects.feature.account.common)
    implementation(projects.feature.account.oauth)
    implementation(projects.feature.account.setup)
    implementation(projects.feature.autodiscovery.api)
    implementation(projects.feature.onboarding.permissions)
    implementation(projects.feature.settings.import)
    implementation(projects.feature.thundermail.api)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
