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
            api(libs.okhttp)
            implementation(libs.okio)
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
    }
}

codeCoverage {
    lineCoverage = 72
}
