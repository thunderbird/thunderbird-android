import org.jetbrains.kotlin.gradle.dsl.JvmTarget

@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.lint) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.jvm) apply false

    id("thunderbird.quality.spotless")
    id("thunderbird.quality.detekt")
    id("thunderbird.dependency.check")
}

val propertyTestCoverage: String? by extra

val javaVersion = JavaVersion.VERSION_11
val jvmTargetVersion = JvmTarget.JVM_11

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
            substitute(module("androidx.fragment:fragment"))
                .using(module("androidx.fragment:fragment:${libs.versions.androidxFragment.get()}"))
            substitute(module("androidx.fragment:fragment-ktx"))
                .using(module("androidx.fragment:fragment-ktx:${libs.versions.androidxFragment.get()}"))
            substitute(module("androidx.appcompat:appcompat"))
                .using(module("androidx.appcompat:appcompat:${libs.versions.androidxAppCompat.get()}"))
            substitute(module("androidx.preference:preference"))
                .using(module("androidx.preference:preference:${libs.versions.androidxPreference.get()}"))
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
            substitute(module("androidx.transition:transition"))
                .using(module("androidx.transition:transition:${libs.versions.androidxTransition.get()}"))
            substitute(module("org.jetbrains:annotations"))
                .using(module("org.jetbrains:annotations:${libs.versions.jetbrainsAnnotations.get()}"))
            substitute(module("org.jetbrains.kotlin:kotlin-stdlib"))
                .using(module("org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}"))
            substitute(module("org.jetbrains.kotlin:kotlin-stdlib-jdk7"))
                .using(module("org.jetbrains.kotlin:kotlin-stdlib-jdk7:${libs.versions.kotlin.get()}"))
            substitute(module("org.jetbrains.kotlin:kotlin-stdlib-jdk8"))
                .using(module("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}"))
            substitute(module("org.jetbrains.kotlinx:kotlinx-coroutines-android"))
                .using(
                    module(
                        "org.jetbrains.kotlinx:kotlinx-coroutines-android:${libs.versions.kotlinxCoroutines.get()}",
                    ),
                )
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = jvmTargetVersion.target
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
    dependsOn(
        subprojects.map { project -> project.tasks.withType(Test::class.java) }
            .flatten()
            .filterNot { task -> task.name in arrayOf("testDebugUnitTest", "test") },
    )
}
