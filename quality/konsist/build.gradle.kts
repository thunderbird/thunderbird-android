plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.test)
}

// Konsist reads the project's sources directly, so declare them as inputs: the tests rerun only when Kotlin
// sources change.
tasks.withType<Test>().configureEach {
    inputs.files(
        fileTree(isolated.rootProject.projectDirectory) {
            include("**/*.kt", "**/*.kts")
            exclude("**/build/**", ".gradle/**", ".kotlin/**")
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("konsistSources")
}
