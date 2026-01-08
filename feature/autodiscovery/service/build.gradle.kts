plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.feature.autodiscovery.autoconfig)

    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kxml2)
}

codeCoverage {
    branchCoverage.set(64)
    lineCoverage.set(60)
}
