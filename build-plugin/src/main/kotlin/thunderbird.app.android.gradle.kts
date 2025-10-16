plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("thunderbird.quality.detekt.typed")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("thunderbird.quality.spotless")
}

android {
    configureSharedConfig(project)

    defaultConfig {
        targetSdk = ThunderbirdProjectConfig.Android.sdkTarget
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = ThunderbirdProjectConfig.Compiler.javaCompatibility.toString()
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar.nio)

    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.koin.bom))

    implementation(libs.bundles.shared.jvm.android.app)

    testImplementation(libs.bundles.shared.jvm.test)
}
