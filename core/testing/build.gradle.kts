plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(libs.assertk)
    implementation(libs.turbine)
}
