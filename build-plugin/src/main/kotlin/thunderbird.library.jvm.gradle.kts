plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

java {
    sourceCompatibility = ThunderbirdProjectConfig.javaVersion
    targetCompatibility = ThunderbirdProjectConfig.javaVersion
}

dependencies {
    testImplementation(libs.bundles.shared.jvm.test)
}
