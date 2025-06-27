plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.dev.mokkery)
}

android {
    namespace = "net.thunderbird.core.logging.file"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.io.core)
            implementation(projects.core.logging.api)
        }
    }
}
