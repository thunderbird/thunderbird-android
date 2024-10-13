plugins {
    id("thunderbird.library.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("thunderbird.quality.detekt.typed")
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
