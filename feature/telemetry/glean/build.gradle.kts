plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "app.k9mail.feature.telemetry.glean"
    resourcePrefix = "telemetry_glean_"
}

dependencies {
    api(projects.feature.telemetry.api)
}
