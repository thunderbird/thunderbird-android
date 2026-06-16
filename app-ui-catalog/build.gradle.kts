plugins {
    id(ThunderbirdPlugins.App.androidCompose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace = "net.thunderbird.ui.catalog"

    defaultConfig {
        applicationId = "net.thunderbird.ui.catalog"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        // Preview build type to render compose without debug features.
        // This gives a better idea of the real world drawing performance.
        create("preview") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".preview"
            isDebuggable = false
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(projects.core.ui.navigation)

    implementation(libs.jetbrains.compose.material3)
    implementation(libs.jetbrains.compose.material.icons.extended)

    implementation(libs.kotlin.reflect)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
