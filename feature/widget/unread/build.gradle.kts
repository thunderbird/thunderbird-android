plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.feature.mail.account.api)

    implementation(projects.legacy.ui.legacy)
    implementation(projects.legacy.core)
    implementation(projects.core.android.account)

    implementation(libs.preferencex)

    testImplementation(libs.robolectric)
}

android {
    namespace = "app.k9mail.feature.widget.unread"
}

codeCoverage {
    branchCoverage.set(10)
    lineCoverage.set(18)
}
