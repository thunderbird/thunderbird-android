plugins {
    id("thunderbird.library.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("thunderbird.quality.detekt.typed")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("thunderbird.quality.spotless")
}

android {
    configureSharedComposeConfig(libs)
}

androidComponents {
    beforeVariants(selector().withBuildType("release")) { variantBuilder ->
        variantBuilder.enableUnitTest = false
        variantBuilder.enableAndroidTest = false
    }
}

dependencies {
    configureSharedComposeDependencies(libs)
}
