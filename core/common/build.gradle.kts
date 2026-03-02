import com.android.build.api.dsl.KotlinMultiplatformAndroidCompilation
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
                // workaround for https://issuetracker.google.com/issues/442950553
                withCompilations { it is KotlinMultiplatformAndroidCompilation }
                withJvm()
            }
        }
    }
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.logging.implLegacy)
            implementation(projects.core.logging.api)
            implementation(projects.core.logging.implFile)
        }
        getByName("commonJvmMain") {
            dependencies {
                implementation(libs.androidx.annotation)
            }
        }
        getByName("commonJvmTest") {
            dependencies {
                implementation(projects.core.logging.testing)
            }
        }
        commonTest.dependencies {
            implementation(projects.core.testing)
        }
        jvmMain.dependencies {
            implementation(libs.androidx.annotation)
        }
    }
}

codeCoverage {
    lineCoverage = 72
}
