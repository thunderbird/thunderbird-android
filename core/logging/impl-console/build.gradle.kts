import com.android.build.api.dsl.KotlinMultiplatformAndroidCompilation
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

plugins {
    id(ThunderbirdPlugins.Library.kmp)
}

kotlin {
    androidLibrary {
        namespace = "net.thunderbird.core.logging.console"
        withHostTest {}
    }
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    applyDefaultHierarchyTemplate {
        common {
            group("commonJvm") {
                // workaround for https://issuetracker.google.com/issues/442950553
                withCompilations { it is KotlinMultiplatformAndroidCompilation }
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

codeCoverage {
    branchCoverage.set(41)
    lineCoverage.set(41)
}
