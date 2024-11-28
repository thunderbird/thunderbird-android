plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.core.ui.compose.designsystem"
    resourcePrefix = "designsystem_"
}

dependencies {
    api(projects.core.ui.compose.theme2.common)

    debugApi(projects.core.ui.compose.theme2.k9mail)
    debugApi(projects.core.ui.compose.theme2.thunderbird)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Landscapist imports a lot of dependencies that we don't need. We exclude them here.
    implementation(libs.lanscapist.coil) {
        exclude(group = "io.coil-kt", module = "coil-gif")
        exclude(group = "io.coil-kt", module = "coil-video")
        exclude(group = "io.coil-kt.coil3", module = "coil-network-ktor3")
        exclude(group = "io.ktor")
    }

    testImplementation(projects.core.ui.compose.testing)
}
