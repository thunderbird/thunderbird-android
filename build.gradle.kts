plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.ksp) apply false

    id("thunderbird.quality.spotless")
    id("thunderbird.dependency.check")
}

val propertyTestCoverage: String? by extra

allprojects {
    extra.apply {
        set("testCoverageEnabled", propertyTestCoverage != null)
    }

    configurations.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("androidx.core:core"))
                .using(module("androidx.core:core:${libs.versions.androidxCore.get()}"))
            substitute(module("androidx.activity:activity"))
                .using(module("androidx.activity:activity:${libs.versions.androidxActivity.get()}"))
            substitute(module("androidx.activity:activity-ktx"))
                .using(module("androidx.activity:activity-ktx:${libs.versions.androidxActivity.get()}"))
            substitute(module("androidx.appcompat:appcompat"))
                .using(module("androidx.appcompat:appcompat:${libs.versions.androidxAppCompat.get()}"))
            substitute(module("androidx.recyclerview:recyclerview"))
                .using(module("androidx.recyclerview:recyclerview:${libs.versions.androidxRecyclerView.get()}"))
            substitute(module("androidx.constraintlayout:constraintlayout"))
                .using(
                    module(
                        "androidx.constraintlayout:constraintlayout:${libs.versions.androidxConstraintLayout.get()}",
                    ),
                )
            substitute(module("androidx.drawerlayout:drawerlayout"))
                .using(module("androidx.drawerlayout:drawerlayout:${libs.versions.androidxDrawerLayout.get()}"))
            substitute(module("androidx.lifecycle:lifecycle-livedata"))
                .using(module("androidx.lifecycle:lifecycle-livedata:${libs.versions.androidxLifecycle.get()}"))
            substitute(module("org.jetbrains:annotations"))
                .using(module("org.jetbrains:annotations:${libs.versions.jetbrainsAnnotations.get()}"))
            substitute(module("org.jetbrains.kotlinx:kotlinx-coroutines-android"))
                .using(
                    module(
                        "org.jetbrains.kotlinx:kotlinx-coroutines-android:${libs.versions.kotlinxCoroutines.get()}",
                    ),
                )
        }
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

tasks.named<Wrapper>("wrapper") {
    gradleVersion = libs.versions.gradle.get()
    distributionType = Wrapper.DistributionType.ALL
}
