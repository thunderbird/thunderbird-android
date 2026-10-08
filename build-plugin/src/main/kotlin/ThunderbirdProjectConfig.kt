import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

object ThunderbirdProjectConfig {

    object Android {
        const val sdkMin = 23

        // Only needed for application
        const val sdkTarget = 36
        const val sdkCompile = 37
    }

    object Compiler {
        val javaCompatibility = JavaVersion.VERSION_17
        val jvmTarget = JvmTarget.JVM_17
    }

    object Testing {

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
         * See: [Robolectric causes native crashes on the JVM](https://github.com/robolectric/robolectric/issues/3202)
         */
        private const val DISABLE_C2_COMPILER = "-XX:TieredStopAtLevel=3"

        /**
         * JVM arguments Robolectric requires on JDK 17+ to reflect into internal OpenJDK classes.
         *
         * Robolectric's `FileDescriptorInterceptor` reaches for `jdk.internal.access.SharedSecrets`
         * when emulating SDK 37, which the JVM module system denies by default. That surfaces as
         * "Failed to interact with raw FileDescriptor internals; perhaps JRE has changed?", caused by
         * an `IllegalAccessException`.
         */
        private const val ADD_OPENS_JDK_INTERNAL_ACCESS_ALL_UNNAMED =
            "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED"

        val robolectricJvmArgs = listOf(
            DISABLE_C2_COMPILER,
            ADD_OPENS_JDK_INTERNAL_ACCESS_ALL_UNNAMED,
        )
    }
}
