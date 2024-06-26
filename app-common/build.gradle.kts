plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    api(projects.app.common)
}

android {
    namespace = "app.k9mail.common"
}
