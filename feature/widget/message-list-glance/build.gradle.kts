plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.compose)
}

android {
    buildFeatures {
        compose = true
    }
    namespace = "net.thunderbird.feature.widget.message.list"
}

dependencies {
    implementation(projects.legacy.ui.legacy)
    implementation(projects.legacy.core)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kotlinx.collections.immutable)

    debugImplementation(libs.androidx.glance.appwidget.preview)
    debugImplementation(libs.androidx.glance.preview)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
