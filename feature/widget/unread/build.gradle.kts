plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.feature.mail.account.api)
    implementation(projects.core.ui.legacy.theme2.common)

    implementation(projects.legacy.ui.legacy)
    implementation(projects.legacy.core)
    implementation(projects.core.android.account)

    implementation(libs.preferencex)

    testImplementation(libs.robolectric)
}

android {
    namespace = "app.k9mail.feature.widget.unread"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

codeCoverage {
    branchCoverage.set(10)
    lineCoverage.set(16)
}
