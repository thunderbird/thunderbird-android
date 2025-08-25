plugins {
    id("com.android.library")
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("thunderbird.quality.detekt.typed")
    id("thunderbird.quality.spotless")
}

kotlin {
    androidTarget {
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

android {
    compileSdk = ThunderbirdProjectConfig.Android.sdkCompile

    defaultConfig {
        minSdk = ThunderbirdProjectConfig.Android.sdkMin
    }

    compileOptions {
        sourceCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
        targetCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
    }

    configureSharedComposeConfig(libs)
}
