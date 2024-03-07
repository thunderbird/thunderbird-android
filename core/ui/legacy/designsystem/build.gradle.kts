plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.core.ui.legacy.designsystem"
}

dependencies {
    // TODO Remove this dependency once the legacy theme is available
    api(libs.android.material)
}
