@file:OptIn(ExperimentalKotlinGradlePluginApi::class)
@file:Suppress("UnstableApiUsage")

import com.android.build.api.withAndroid
import net.thunderbird.gradle.plugin.featureflag.task.registerGenerateFeatureFlagRawResTask
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id(ThunderbirdPlugins.Library.kmp)
    alias(libs.plugins.tb.featureflag.library)
}

featureFlag {
    catalog.set(layout.settingsDirectory.file("config/featureflag/thunderbird_mobile_featureflag.catalog.json"))
}

kotlin {
    applyDefaultHierarchyTemplate {
        common {
            group("commonJvm") {
                withAndroid()
                withJvm()
            }
        }
    }
    android {
        namespace = "net.thunderbird.core.featureflag"
        // Required so the generated `res/raw` catalog is merged into the module's Android resources.
        androidResources.enable = true
    }

    sourceSets {
        val commonJvmMain = getByName("commonJvmMain")
        commonMain.dependencies {
            api(projects.core.configstore.api)
            implementation(projects.core.file)
            implementation(projects.core.logging.api)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.json)
        }
        commonJvmMain.dependencies {
            implementation(libs.ktor.client.cio)
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
