plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.logging.config"
}

kotlin {
    sourceSets {
        androidMain.dependencies {
            implementation(libs.timber)
        }
        commonMain.dependencies {
            api(projects.core.logging.api)
            implementation(projects.core.logging.implComposite)
            implementation(projects.core.logging.implFile)
        }
    }
}
