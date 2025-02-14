plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "com.example.dddonboarding"
    compileSdk = 34
}

dependencies {
    implementation(projects.core.ui.compose.common)
    implementation(projects.core.ui.compose.designsystem)
    implementation("androidx.compose.material:material:1.7.7")
    implementation(projects.core.common)
    implementation(projects.feature.account.common)
    implementation(project(":mail:common"))
    implementation(project(":feature:account:setup"))
}
