plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = ThunderbirdProjectConfig.javaVersion
    targetCompatibility = ThunderbirdProjectConfig.javaVersion
}

dependencies {
    implementation(libs.bundles.shared.jvm.main)
    testImplementation(libs.bundles.shared.jvm.test)
}
