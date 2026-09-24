import com.android.build.api.variant.HostTestBuilder
import gradle.kotlin.dsl.accessors._b13b8a551825b1ea90bfd0b44424026a.koinCompiler

plugins {
    id("thunderbird.library.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("net.thunderbird.gradle.plugin.quality.detekt")
    id("net.thunderbird.gradle.plugin.quality.spotless")
    id("io.insert-koin.compiler.plugin")
}

android {
    testOptions {
        // AGP's Compose Preview task (since AGP 9.5) requires Android resources in unit tests, like the KMP setup
        unitTests.isIncludeAndroidResources = true
    }
}

androidComponents {
    beforeVariants { variantBuilder ->
        // AGP's Compose Preview task (since AGP 9.5) requires unit tests for every variant
        @Suppress("UnstableApiUsage")
        variantBuilder.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = true
    }
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        variantBuilder.enableAndroidTest = false
    }
}

dependencies {
    val isComponentsBuild = rootProject.name == "components"
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.shared.android)
    implementation(libs.bundles.shared.android.compose)
    implementation(libs.bundles.shared.tmc.components)

    debugImplementation(libs.bundles.shared.android.compose.debug)

    testImplementation(libs.bundles.shared.android.test)
    testImplementation(libs.bundles.shared.android.compose.test)
    testImplementation(libs.bundles.shared.tmc.components.test)

    androidTestImplementation(libs.bundles.shared.android.compose.androidTest)

    if (!isComponentsBuild) {
        implementation(libs.tb.mobile.components.ui.bolt)
        testImplementation(libs.tb.mobile.components.ui.testing)
    }
}

koinCompiler {
    compileSafety = true
    strictSafety = true
    userLogs = true
    debugLogs = false
    unsafeDslChecks = true
}
