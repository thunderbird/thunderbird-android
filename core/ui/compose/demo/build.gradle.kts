plugins {
    id(ThunderbirdPlugins.App.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.demo"

    defaultConfig {
        applicationId = "app.k9mail.core.ui.compose.demo"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    androidTestImplementation(libs.androidx.test.ext.junit.ktx)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
