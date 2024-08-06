plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.ui.account"
}

dependencies {
    api(projects.legacy.account)

    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)

    implementation(libs.androidx.lifecycle.livedata.ktx)
}
