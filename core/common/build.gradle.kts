plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.common"
}

kotlin {
    sourceSets {
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
