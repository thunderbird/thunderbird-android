plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.ui.folder"
}

dependencies {
    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.core.android.account)
    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)
    implementation(projects.feature.folder.api)

    implementation(libs.androidx.lifecycle.livedata.ktx)
}
