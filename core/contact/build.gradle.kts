plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.contact"
}

dependencies {
    implementation(projects.mail.common)
}
