import net.thunderbird.gradle.plugin.ProjectConfig

plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("net.thunderbird.gradle.plugin.quality.detekt")
    id("net.thunderbird.gradle.plugin.quality.spotless")
}

java {
    sourceCompatibility = ProjectConfig.Compiler.javaCompatibility
    targetCompatibility = ProjectConfig.Compiler.javaCompatibility
}

configureKotlinJavaCompatibility()

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.koin.bom))
    implementation(platform(libs.tb.mobile.components.bom))

    implementation(libs.bundles.shared.jvm)
    implementation(libs.bundles.shared.tmc.components)

    testImplementation(libs.bundles.shared.jvm.test)
    testImplementation(libs.bundles.shared.tmc.components.test)
}

tasks.register("testsOnCi") {
    description = "Run all tests except the unit tests on the release build."
    dependsOn(
        tasks.withType<Test>().matching {
            it.name != "testReleaseUnitTest"
        },
    )
}
