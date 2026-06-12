plugins {
    id("application")
    id("org.jetbrains.kotlin.jvm")
    id("net.thunderbird.gradle.plugin.quality.coverage")
    id("net.thunderbird.gradle.plugin.quality.detekt")
    id("net.thunderbird.gradle.plugin.quality.spotless")
}

java {
    sourceCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
    targetCompatibility = ThunderbirdProjectConfig.Compiler.javaCompatibility
}

configureKotlinJavaCompatibility()

dependencies {
    implementation(platform(libs.kotlin.bom))
    implementation(platform(libs.koin.bom))

    implementation(libs.bundles.shared.jvm)
    testImplementation(libs.bundles.shared.jvm.test)
}

tasks.register("testsOnCi") {
    dependsOn(
        tasks.withType<Test>().matching {
            it.name != "testReleaseUnitTest"
        }
    )
}
