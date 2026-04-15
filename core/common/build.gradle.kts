import com.android.build.api.withAndroid
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    android {
        namespace = "net.thunderbird.core.common"
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("commonJvm") {
                @Suppress("UnstableApiUsage")
                withAndroid()
                withJvm()
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.implLegacy)
            implementation(projects.core.logging.api)
            implementation(projects.core.logging.implFile)
            implementation(libs.koin.compose)
        }
        getByName("commonJvmTest") {
            dependencies {
                implementation(projects.core.logging.testing)
            }
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}

codeCoverage {
    lineCoverage = 72
}
