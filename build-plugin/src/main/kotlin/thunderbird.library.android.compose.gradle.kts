plugins {
    id("thunderbird.library.android")
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
