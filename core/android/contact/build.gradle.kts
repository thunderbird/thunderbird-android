plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.android.contact"
}

dependencies {
    implementation(projects.mail.common)
}
