plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.ui.animation.manager"
}

dependencies {
    implementation(projects.core.preference.api)
}
