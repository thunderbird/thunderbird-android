plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.ui.account"
}

dependencies {
    implementation(projects.core.ui.legacy.designsystem)

    implementation(libs.glide)
}
