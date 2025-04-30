plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "net.discdd.k9"
    compileSdk = 34

    defaultConfig {
        applicationId = "net.discdd.k9"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.moshi)
    implementation(libs.client.adapter)
    ksp(libs.moshi.kotlin.codegen)

    api(projects.backend.api)
    api(projects.mail.common)

    testImplementation(projects.mail.testing)
}
