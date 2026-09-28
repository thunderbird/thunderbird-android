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

    sourceSets {
        commonMain.dependencies {
            api(projects.core.configstore.api)
            implementation(projects.core.logging.api)
        }
        commonTest.dependencies {
            implementation(projects.core.configstore.testing)
            implementation(projects.core.logging.testing)
        }
        androidHostTest.dependencies {
            implementation(libs.robolectric)
        }
    }
}

codeCoverage {
    lineCoverage = 60
}

tasks.registerGenerateFeatureFlagRawResTask(project = project)
