plugins {
    id(ThunderbirdPlugins.Library.android)
    alias(libs.plugins.compose)
}

android {
    buildFeatures {
        compose = true
    }
    namespace = "net.thunderbird.feature.widget.message.list"

    testOptions {
        // AGP's Compose Preview task (since AGP 9.5) requires Android resources in unit tests
        unitTests.isIncludeAndroidResources = true
    }
}

androidComponents {
    beforeVariants { variantBuilder ->
        // AGP's Compose Preview task (since AGP 9.5) requires unit tests for every variant
        @Suppress("UnstableApiUsage")
        variantBuilder.hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
}

dependencies {
    implementation(projects.legacy.core)
    implementation(projects.legacy.logging)
    implementation(projects.legacy.ui.legacy)

    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.kotlinx.collections.immutable)

    debugImplementation(libs.androidx.glance.appwidget.preview)
    debugImplementation(libs.androidx.glance.preview)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
