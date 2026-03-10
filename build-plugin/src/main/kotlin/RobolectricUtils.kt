import com.android.build.gradle.BaseExtension

/**
 * Disables the C2 compiler for Robolectric tests.
 *
 * This is a workaround for a known issue where Robolectric tests can fail on JDK 17+
 * with a "failed to compile" error. The issue is related to the Tiered Compilation in the JVM,
 * specifically the C2 (server) compiler. Disabling C2 forces the JVM to use the C1 (client)
 * compiler, which avoids the problem.
 *
 * The official workaround uses `-XX:+TieredCompilation -XX:TieredStopAtLevel=1`, but just
 * `-XX:TieredStopAtLevel=3` seems to work. In case the flakiness still happens, we can
 * use the workaround mentioned in the issue.
 *
 * See: https://github.com/robolectric/robolectric/issues/3202
 */
fun BaseExtension.disableC2CompilerForRobolectric() {
    testOptions {
        unitTests.all {
            it.jvmArgs("-XX:TieredStopAtLevel=3")
        }
    }
}
