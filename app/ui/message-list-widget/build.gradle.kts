plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.app.ui.legacy)
    implementation(projects.app.core)

    implementation(libs.timber)
}

android {
    namespace = "app.k9mail.ui.widget.list"

    buildFeatures {
        buildConfig = true
    }
}
