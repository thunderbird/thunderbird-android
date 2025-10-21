plugins {
    id(ThunderbirdPlugins.Library.kmpCompose)
    alias(libs.plugins.dev.mokkery)
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.common)
            implementation(projects.core.featureflag)
            implementation(projects.core.outcome)
        }
        commonTest.dependencies {
            implementation(projects.feature.notification.testing)
        }
        androidMain.dependencies {
            implementation(projects.core.ui.compose.designsystem)
            implementation(projects.core.ui.compose.theme2.common)
        }
        androidUnitTest.dependencies {
            implementation(projects.core.ui.compose.testing)
            implementation(libs.bundles.shared.jvm.test.compose)
            implementation(libs.bundles.shared.jvm.android.compose.debug)
        }
        jvmTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.bundles.shared.jvm.test)
        }
    }

    sourceSets.all {
        compilerOptions {
            freeCompilerArgs.addAll(
                "-Xexpect-actual-classes",
                "-Xwhen-guards",
            )
        }
    }
}

android {
    namespace = "net.thunderbird.feature.notification.api"

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

java {
    sourceCompatibility = ThunderbirdProjectConfig.Compiler.javaVersion
    targetCompatibility = ThunderbirdProjectConfig.Compiler.javaVersion
}

compose.resources {
    publicResClass = false
    packageOfResClass = "net.thunderbird.feature.notification.resources.api"
}
