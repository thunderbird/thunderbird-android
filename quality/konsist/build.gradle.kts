import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.test)
}

// Konsist reads the project's sources directly, so declare them as inputs: the tests rerun only when Kotlin
// sources change. The matched files are declared instead of the tree, because a tree rooted at the project
// directory overlaps the outputs of other tasks (e.g. dependencyGuard) and Gradle reports implicit dependencies.
tasks.withType<Test>().configureEach {
    inputs.files(
        provider {
            fileTree(isolated.rootProject.projectDirectory) {
                include("**/*.kt", "**/*.kts")
                exclude("**/build/**", ".gradle/**", ".kotlin/**")
            }.files
        },
    ).withPathSensitivity(PathSensitivity.RELATIVE)
        .withPropertyName("konsistSources")

    // Print Konsist's violation report (rule, message, offending declarations) to the console instead of only the
    // exception class.
    testLogging {
        events(TestLogEvent.FAILED)
        exceptionFormat = TestExceptionFormat.FULL
        showStackTraces = false
    }
}
