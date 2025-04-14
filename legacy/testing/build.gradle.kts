plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.legacy.core)
}

android {
    namespace = "com.fsck.k9.testing"
}
