plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.logging.config"
    }
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
