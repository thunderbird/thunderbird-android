import com.android.build.api.variant.HostTestBuilder

plugins {
    id("thunderbird.library.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("thunderbird.quality.detekt.typed")
    id("thunderbird.quality.kover")
    id("thunderbird.quality.spotless")
}

android {
    configureSharedComposeConfig(libs)
}

androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        variantBuilder.hostTests[HostTestBuilder.UNIT_TEST_TYPE]?.enable = false
        variantBuilder.enableAndroidTest = false
    }
}

dependencies {
    configureSharedComposeDependencies(libs)
}
