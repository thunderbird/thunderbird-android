plugins {
    id(ThunderbirdPlugins.Library.androidCompose)
}

android {
    namespace = "app.k9mail.feature.onboarding.migration.noop"
    resourcePrefix = "onboarding_migration_noop_"
}

dependencies {
    api(projects.feature.onboarding.migration.api)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
