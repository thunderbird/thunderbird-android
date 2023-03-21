plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.testing"
}

dependencies {
    implementation(projects.core.ui.compose.theme)
    implementation(libs.androidx.compose.material)

    implementation(libs.bundles.shared.jvm.test.compose)
}
