plugins {
    id(ThunderbirdPlugins.App.androidCompose)
}

android {
    namespace = "app.k9mail.ui.catalog"

    defaultConfig {
        applicationId = "app.k9mail.ui.catalog"
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
            isDefault = true
        }
    }
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)
    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.core.ui.compose.theme2.thunderbird)
    implementation(projects.core.ui.compose.theme2.k9mail)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.datetime)
}
