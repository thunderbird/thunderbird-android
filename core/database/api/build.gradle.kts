plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.database"
    }
    sourceSets {
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}
