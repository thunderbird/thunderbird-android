plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(projects.legacy.common)
}

android {
    namespace = "app.k9mail.common"
}
