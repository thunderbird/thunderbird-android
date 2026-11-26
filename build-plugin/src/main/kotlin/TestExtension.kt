import org.gradle.api.Project

/**
 * Disables compiling and running the release unit tests.
 *
 * Use this for modules whose unit tests depend on code that is only available for the debug build type,
 * e.g. fakes in `src/debug` or `debugImplementation` dependencies. The release unit test variant itself stays
 * enabled, because AGP's Compose Preview task (since AGP 9.5) requires it.
 */
fun Project.disableReleaseUnitTests() {
    tasks.matching { task ->
        task.name in setOf(
            "compileReleaseUnitTestKotlin",
            "compileReleaseUnitTestJavaWithJavac",
            "testReleaseUnitTest",
        )
    }.configureEach {
        enabled = false
    }
}
