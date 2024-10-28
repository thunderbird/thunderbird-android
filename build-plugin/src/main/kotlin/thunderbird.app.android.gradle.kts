plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("thunderbird.quality.detekt.typed")
    id("thunderbird.quality.spotless")
}

android {
    configureSharedConfig()

    defaultConfig {
        targetSdk = ThunderbirdProjectConfig.androidSdkTarget
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = ThunderbirdProjectConfig.javaCompatibilityVersion.toString()
    }

    lint {
        checkDependencies = true
        lintConfig = file("${rootProject.projectDir}/config/lint/lint.xml")
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    coreLibraryDesugaring(libs.android.desugar)

    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.koin.bom))

    implementation(libs.bundles.shared.jvm.android.app)

    testImplementation(libs.bundles.shared.jvm.test)
}
