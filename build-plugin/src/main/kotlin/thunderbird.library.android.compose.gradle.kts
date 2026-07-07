import com.android.build.api.variant.HostTestBuilder

plugins {
    id("thunderbird.library.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("net.thunderbird.gradle.plugin.quality.detekt")
    id("net.thunderbird.gradle.plugin.quality.spotless")
}

androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        @Suppress("UnstableApiUsage")
        variantBuilder.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
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

    debugImplementation(libs.bundles.shared.android.compose.debug)

    testImplementation(libs.bundles.shared.android.test)
    testImplementation(libs.bundles.shared.android.compose.test)

    androidTestImplementation(libs.bundles.shared.android.compose.androidTest)

    if (!isComponentsBuild) {
        implementation(libs.tb.mobile.components.ui.bolt)
        testImplementation(libs.tb.mobile.components.ui.testing)
    }
}
