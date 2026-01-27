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
    implementation(projects.core.ui.compose.navigation)

    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.core.ui.compose.theme2.thunderbird)
    implementation(projects.core.ui.compose.theme2.k9mail)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlin.reflect)
    implementation(libs.kotlinx.datetime)
}
