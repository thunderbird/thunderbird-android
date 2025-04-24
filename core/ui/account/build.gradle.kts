plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.ui.account"
}

dependencies {
    implementation(projects.core.ui.legacy.designsystem)

    implementation(libs.glide)
}
