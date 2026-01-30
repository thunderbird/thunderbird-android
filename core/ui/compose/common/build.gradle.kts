plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.common"
    resourcePrefix = "core_ui_common_"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.window)
    implementation(projects.core.logging.api)

    implementation(projects.core.common)
    implementation(projects.core.logging.api)
    testImplementation(projects.core.logging.testing)
    testImplementation(projects.core.ui.compose.testing)
    testImplementation(projects.core.ui.compose.designsystem)
    testImplementation(projects.core.android.testing)
    testImplementation(projects.core.logging.testing)
}

codeCoverage {
    branchCoverage = 2
    lineCoverage = 8
}
