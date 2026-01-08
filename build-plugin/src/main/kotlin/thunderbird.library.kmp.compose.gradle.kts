plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("thunderbird.quality.detekt.typed")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("thunderbird.quality.spotless")
}

kotlin {
    androidLibrary {
        compileSdk = ThunderbirdProjectConfig.Android.sdkCompile
        minSdk = ThunderbirdProjectConfig.Android.sdkMin

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
            implementation(libs.bundles.shared.kmp.compose)

            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }

        commonTest.dependencies {
            implementation(libs.bundles.shared.kmp.common.test)
        }

        androidMain.dependencies {
            implementation(libs.bundles.shared.kmp.android)
            implementation(libs.bundles.shared.kmp.compose.android)
            implementation(compose.preview)
        }
    }
}

configureKotlinJavaCompatibility()
