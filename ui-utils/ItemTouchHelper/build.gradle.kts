plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(libs.androidx.recyclerview)
}

android {
    namespace = "app.k9mail.ui.utils.itemtouchhelper"
}
