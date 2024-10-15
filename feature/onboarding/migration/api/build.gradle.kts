plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.k9mail.feature.onboarding.migration.api"
    resourcePrefix = "onboarding_migration_api_"
}

dependencies {
    api(projects.core.ui.compose.navigation)
}
