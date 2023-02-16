plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    configureSharedConfig()

    defaultConfig {
        @Suppress("DEPRECATION")
        targetSdk = ThunderbirdProjectConfig.androidSdkTarget
    }

    buildFeatures {
        buildConfig = false
    }

    kotlinOptions {
        jvmTarget = ThunderbirdProjectConfig.javaVersion.toString()
    }

    lint {
        lintConfig = file("${rootProject.projectDir}/config/lint/lint.xml")
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    testImplementation(libs.bundles.shared.jvm.test)
}
