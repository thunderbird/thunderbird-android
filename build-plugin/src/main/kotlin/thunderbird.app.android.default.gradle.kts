plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    compileSdk = ThunderbirdProjectConfig.androidSdkCompile

    defaultConfig {
        minSdk = ThunderbirdProjectConfig.androidSdkMin
        targetSdk = ThunderbirdProjectConfig.androidSdkTarget

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true

        sourceCompatibility = ThunderbirdProjectConfig.javaVersion
        targetCompatibility = ThunderbirdProjectConfig.javaVersion
    }

    kotlinOptions {
        jvmTarget = ThunderbirdProjectConfig.javaVersion.toString()
    }

    lint {
        checkDependencies = true

        abortOnError = false
        lintConfig = file("${rootProject.projectDir}/config/lint/lint.xml")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar)

    testImplementation(libs.bundles.shared.jvm.test)
}
