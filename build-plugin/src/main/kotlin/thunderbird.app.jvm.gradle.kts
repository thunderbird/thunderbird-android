plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = ThunderbirdProjectConfig.javaCompatibilityVersion
    targetCompatibility = ThunderbirdProjectConfig.javaCompatibilityVersion
}

configureKotlinJavaCompatibility()

dependencies {
    implementation(libs.bundles.shared.jvm.main)
    testImplementation(libs.bundles.shared.jvm.test)
}
