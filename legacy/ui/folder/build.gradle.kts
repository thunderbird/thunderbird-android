plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.ui.folder"
}

dependencies {
    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.core.mail.folder.api)

    implementation(projects.core.account)
    implementation(projects.legacy.account)
    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)
    implementation(projects.feature.folder.api)

    implementation(libs.androidx.lifecycle.livedata.ktx)
}
