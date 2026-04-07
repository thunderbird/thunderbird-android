plugins {
    id("com.android.kotlin.multiplatform.library")
    id("org.jetbrains.kotlin.multiplatform")
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
        }

        commonTest.dependencies {
            implementation(libs.bundles.shared.kmp.common.test)
        }

        androidMain.dependencies {
            implementation(libs.bundles.shared.kmp.android)
        }

        androidHostTest.dependencies {
            implementation(libs.bundles.shared.kmp.android.test)
        }

        jvmMain.dependencies {
            implementation(libs.bundles.shared.kmp.jvm)
        }

        jvmTest.dependencies {
            implementation(libs.bundles.shared.kmp.jvm.test)
        }
    }
}

configureKotlinJavaCompatibility()
