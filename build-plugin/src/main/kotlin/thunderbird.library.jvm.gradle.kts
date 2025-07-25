import org.gradle.jvm.tasks.Jar

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("thunderbird.quality.detekt.typed")
    id("thunderbird.quality.kover")
    id("thunderbird.quality.spotless")
}

java {
    sourceCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
    targetCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
}

tasks.withType<Jar> {
    // We want to avoid ending up with multiple JARs having the same name, e.g. "common.jar".
    // To do this, we use the modified project path as base name, e.g. ":core:common" -> "core.common".
    val projectDotPath = project.path.split(":").filter { it.isNotEmpty() }.joinToString(separator = ".")
    archiveBaseName.set(projectDotPath)
}

configureKotlinJavaCompatibility()

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.koin.bom))

    implementation(libs.bundles.shared.jvm.main)
    testImplementation(libs.bundles.shared.jvm.test)
}
