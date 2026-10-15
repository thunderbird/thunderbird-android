plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.feature.account.internal"
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.feature.account.api)
            implementation(projects.core.common)
        }
    }
}
