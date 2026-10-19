plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.benchmarks"
}

dependencies {
    api(libs.androidx.benchmark.macro.junit4)
    api(libs.androidx.test.ext.junit.ktx)
    api(libs.androidx.test.runner)
    api(libs.androidx.test.uiautomator)

    // To avoid test tag re-declaration, I'm adding these modules as an exception
    // to the definition on ADR-0007, where prohibt libraries to depend on features.
    implementation(projects.feature.account.common)
    implementation(projects.feature.account.server.certificate)
    implementation(projects.feature.account.server.settings)
    implementation(projects.feature.account.setup)
    implementation(projects.feature.onboarding.permissions)
    implementation(projects.feature.onboarding.welcome)
    implementation(projects.feature.thundermail.api)
}
