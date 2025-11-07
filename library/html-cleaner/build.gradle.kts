plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
}

dependencies {
    implementation(libs.jsoup)
    implementation(projects.core.featureflag)
    implementation(projects.feature.mail.message.reader.api)
}
