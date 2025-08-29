plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.jetbrains.compose) apply false

    id("thunderbird.quality.spotless.root")
    id("thunderbird.dependency.check")
}

val propertyTestCoverage: String? by extra

allprojects {
    extra.apply {
        set("testCoverageEnabled", propertyTestCoverage != null)
    }

    tasks.withType<Test> {
        testLogging {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            showCauses = true
            showExceptions = true
            showStackTraces = true
        }
    }
}

tasks.register("testsOnCi") {
    val skipTests = setOf("testReleaseUnitTest")

    dependsOn(
        subprojects.map { project -> project.tasks.withType(Test::class.java) }
            .flatten()
            .filterNot { task -> task.name in skipTests },
    )
}

tasks.register("buildCliTools") {
    val cliToolsProjects = subprojects.filter { it.path.startsWith(":cli:") }
    dependsOn(
        cliToolsProjects.map { project -> project.tasks.named("build") },
    )
}

tasks.named<Wrapper>("wrapper") {
    gradleVersion = libs.versions.gradle.get()
    distributionType = Wrapper.DistributionType.ALL
}
