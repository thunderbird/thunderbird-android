plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.ui.account"
}

dependencies {
    api(projects.legacy.account)

    implementation(projects.core.android.common)
    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)

    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.glide)
}
