plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("net.thunderbird.gradle.plugin.quality.detekt")
    id("net.thunderbird.gradle.plugin.quality.spotless")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    android {
        compileSdk = ThunderbirdProjectConfig.Android.sdkCompile
        minSdk = ThunderbirdProjectConfig.Android.sdkMin

        androidResources.enable = true

        withHostTest {
            isIncludeAndroidResources = true
        }

        compilerOptions {
            jvmTarget.set(ThunderbirdProjectConfig.Compiler.jvmTarget)
        }
    }

    jvm {
        compilerOptions {
            jvmTarget.set(ThunderbirdProjectConfig.Compiler.jvmTarget)
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project.dependencies.platform(libs.kotlin.bom))
            implementation(project.dependencies.platform(libs.koin.bom))
            implementation(libs.bundles.shared.kmp.common)
            implementation(libs.bundles.shared.kmp.compose.common)

            implementation(libs.jetbrains.compose.runtime)
            implementation(libs.jetbrains.compose.foundation)
            implementation(libs.jetbrains.compose.ui)
            implementation(libs.jetbrains.compose.components.resources)
            implementation(libs.jetbrains.compose.components.ui.preview)
        }

        commonTest.dependencies {
            implementation(libs.bundles.shared.kmp.common.test)
            implementation(libs.bundles.shared.kmp.compose.common.test)
        }

        androidMain.dependencies {
            implementation(libs.bundles.shared.kmp.android)
            implementation(libs.bundles.shared.kmp.compose.android)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.shared.kmp.android.test)
            implementation(libs.bundles.shared.kmp.compose.android.test)
        }

        jvmMain.dependencies {
            implementation(libs.bundles.shared.kmp.jvm)
            implementation(libs.bundles.shared.kmp.compose.jvm)
        }

        jvmTest.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.bundles.shared.kmp.jvm.test)
            implementation(libs.bundles.shared.kmp.compose.jvm.test)
        }
    }
}

configureKotlinJavaCompatibility()

tasks.register("testsOnCi") {
    dependsOn(
        tasks.withType<Test>()
    )
}
