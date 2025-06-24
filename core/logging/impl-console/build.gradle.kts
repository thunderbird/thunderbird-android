import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

android {
    namespace = "net.thunderbird.core.logging.console"
}

kotlin {
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("commonJvm") {
                withAndroidTarget()
                withJvm()
            }
        }
    }
    sourceSets {
        val commonJvmMain by getting

        commonMain.dependencies {
            implementation(projects.core.logging.api)
        }

        androidMain.dependencies {
            implementation(libs.timber)
        }
    }
}
