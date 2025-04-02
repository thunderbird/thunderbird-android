plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.ui.theme.manager"
}

dependencies {
    api(projects.core.ui.theme.api)

    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.legacy.preferences)
}
