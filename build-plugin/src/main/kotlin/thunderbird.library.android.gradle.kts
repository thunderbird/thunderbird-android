plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    configureSharedConfig()

    buildFeatures {
        buildConfig = false
    }

    kotlinOptions {
        jvmTarget = ThunderbirdProjectConfig.javaCompatibilityVersion.toString()
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
    implementation(libs.bundles.shared.jvm.main)
    implementation(libs.bundles.shared.jvm.android)

    testImplementation(libs.bundles.shared.jvm.test)
}
