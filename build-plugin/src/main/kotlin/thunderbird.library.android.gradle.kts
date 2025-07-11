import org.jetbrains.kotlin.gradle.internal.config.LanguageFeature

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("thunderbird.quality.detekt.typed")
    id("thunderbird.quality.spotless")
}

android {
    configureSharedConfig(project)

    buildFeatures {
        buildConfig = false
    }

    kotlinOptions {
        jvmTarget = ThunderbirdProjectConfig.Compiler.javaCompatibility.toString()
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

kotlin {
    sourceSets.all {
        languageSettings.enableLanguageFeature(LanguageFeature.WhenGuards.name)
    }
}

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.koin.bom))

    implementation(libs.bundles.shared.jvm.main)
    implementation(libs.bundles.shared.jvm.android)

    testImplementation(libs.bundles.shared.jvm.test)
}
