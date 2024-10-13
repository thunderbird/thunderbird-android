plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.k9mail.feature.funding.api"
    resourcePrefix = "funding_api_"
}

dependencies {
    api(projects.core.ui.compose.navigation)
}
