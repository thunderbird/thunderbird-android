import net.thunderbird.gradle.plugin.featureflag.task.registerGenerateFeatureFlagRawResTask

plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.tb.featureflag.library)
}

featureFlag {
    catalog.set(layout.settingsDirectory.file("config/featureflag/thunderbird_mobile_featureflag.catalog.json"))
}

kotlin {
    android {
        namespace = "net.thunderbird.core.featureflag"
        // Required so the generated `res/raw` catalog is merged into the module's Android resources.
        androidResources.enable = true
    }
}

codeCoverage {
    lineCoverage = 60
}

tasks.registerGenerateFeatureFlagRawResTask(project = project)
