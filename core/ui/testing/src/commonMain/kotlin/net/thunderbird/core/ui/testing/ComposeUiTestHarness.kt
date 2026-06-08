package net.thunderbird.core.ui.testing

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Platform-agnostic test harness for Compose UI tests.
 *
 * The harness wraps the platform-specific [androidx.compose.ui.test.v2.runComposeUiTest] implementation and exposes a
 * common [ComposeUiTestScope].
 */
public expect abstract class ComposeUiTestHarness() {

    /**
     * Runs a Compose UI test.
     *
     * The parameters mirror [androidx.compose.ui.test.v2.runComposeUiTest]. [effectContext] is used for composition,
     * `LaunchedEffect`, `rememberCoroutineScope`, and the main test clock. [runTestContext] is used for the test block.
     * Compose requires these contexts to not share a [kotlinx.coroutines.test.TestCoroutineScheduler].
     *
     * @param effectContext The [CoroutineContext] to use for the [androidx.compose.ui.test.v2.runComposeUiTest] implementation.
     * @param runTestContext The [kotlinx.coroutines.test.StandardTestDispatcher] to use for the [androidx.compose.ui.test.v2.runComposeUiTest] implementation.
     * @param testTimeout The timeout for the test, defaults to 60 seconds.
     * @param block The block of code to execute within the Compose UI test harness.
     */
    public fun runComposeTest(
        effectContext: CoroutineContext = EmptyCoroutineContext,
        runTestContext: CoroutineContext = EmptyCoroutineContext,
        testTimeout: Duration = 60.seconds,
        block: suspend ComposeUiTestScope.() -> Unit,
    )
}
