plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.preference.impl"
}

dependencies {
    api(projects.core.preference.api)
    implementation(projects.core.logging.api)
}
