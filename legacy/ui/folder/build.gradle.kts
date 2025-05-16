plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.legacy.ui.folder"
}

dependencies {
    implementation(projects.core.ui.legacy.designsystem)

    implementation(projects.legacy.account)
    implementation(projects.legacy.mailstore)
    implementation(projects.legacy.message)
    implementation(projects.feature.mail.account.api)
    implementation(projects.feature.mail.folder.api)

    implementation(libs.androidx.lifecycle.livedata.ktx)
}
