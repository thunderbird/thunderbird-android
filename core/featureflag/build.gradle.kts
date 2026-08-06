import net.thunderbird.gradle.plugin.featureflag.task.registerGenerateFeatureFlagRawResTask

plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.featureflag"
        // Required so the generated `res/raw` catalog is merged into the module's Android resources.
        androidResources.enable = true
    }

    sourceSets {
        androidHostTest.dependencies {
            implementation(libs.robolectric)
        }
    }
}

codeCoverage {
    lineCoverage = 60
}

tasks.registerGenerateFeatureFlagRawResTask(project = project)
