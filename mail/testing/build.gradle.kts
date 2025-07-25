plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    api(projects.mail.common)
    api(projects.core.common)

    api(libs.okio)
    api(libs.junit)
    api(libs.assertk)
}
