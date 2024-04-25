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
}

dependencies {
    implementation(projects.core.ui.compose.designsystem)

    implementation(projects.core.ui.compose.theme2.thunderbird)
    implementation(projects.core.ui.compose.theme2.k9mail)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    implementation(libs.kotlinx.datetime)
}
