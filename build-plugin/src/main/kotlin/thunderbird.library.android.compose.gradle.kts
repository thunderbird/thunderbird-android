plugins {
    id("thunderbird.library.android")
}

android {
    configureSharedComposeConfig(libs)
}

dependencies {
    configureSharedComposeDependencies(libs)
}
