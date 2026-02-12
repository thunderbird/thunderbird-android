import com.android.build.api.variant.HostTestBuilder

plugins {
    id("thunderbird.library.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("thunderbird.quality.detekt.typed")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("thunderbird.quality.spotless")
}

androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        @Suppress("UnstableApiUsage")
        variantBuilder.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
        variantBuilder.enableAndroidTest = false
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.bundles.shared.jvm.android.compose)

    debugImplementation(libs.bundles.shared.jvm.android.compose.debug)

    testImplementation(libs.bundles.shared.jvm.test.compose)

    androidTestImplementation(libs.bundles.shared.jvm.androidtest.compose)
}
