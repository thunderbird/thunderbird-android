plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.funding.googleplay"
    resourcePrefix = "funding_googleplay_"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    api(projects.feature.funding.api)

    implementation(projects.core.common)
    implementation(projects.core.outcome)
    implementation(projects.core.logging.api)
    implementation(projects.core.ui.compose.designsystem)

    implementation(libs.android.billing)
    implementation(libs.android.billing.ktx)
    implementation(libs.android.material)

    testImplementation(projects.core.testing)
    testImplementation(projects.core.ui.compose.testing)

    testImplementation(libs.androidx.lifecycle.runtime.testing)
    testImplementation(libs.androidx.fragment.testing)
}
