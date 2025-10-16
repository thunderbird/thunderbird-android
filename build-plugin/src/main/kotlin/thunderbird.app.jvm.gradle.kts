plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm")
    id("thunderbird.quality.detekt.typed")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("thunderbird.quality.spotless")
}

java {
    sourceCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
    targetCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
}

configureKotlinJavaCompatibility()

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.koin.bom))

    implementation(libs.bundles.shared.jvm.main)
    testImplementation(libs.bundles.shared.jvm.test)
}
