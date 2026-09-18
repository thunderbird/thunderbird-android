plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.app.composition"
    }

    sourceSets {
        commonMain.dependencies {
            implementation(projects.feature.account.core)
        }
    }
}
