package net.thunderbird.core.ui.testing

import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.test.TestDispatcher

/**
 * Platform-agnostic test harness for Compose UI tests.
 *
 * The harness wraps the platform-specific [androidx.compose.ui.test.v2.runComposeUiTest] implementation and exposes a
 * common [ComposeUiTestScope].
 *
 * @param mainDispatcher Optional dispatcher to install as [kotlinx.coroutines.Dispatchers.Main] for each
 * [runComposeTest] call. When set and [runComposeTest] uses the default [effectContext], the same dispatcher is also
 * used as Compose's effect context.
 */
public expect abstract class ComposeUiTestHarness(
    mainDispatcher: TestDispatcher? = null,
) {

    /**
     * Runs a Compose UI test.
     *
     * The parameters mirror [androidx.compose.ui.test.v2.runComposeUiTest]. [effectContext] is used for composition,
     * `LaunchedEffect`, `rememberCoroutineScope`, and the main test clock. [runTestContext] is used for the test block.
     * Compose requires these contexts to not share a [kotlinx.coroutines.test.TestCoroutineScheduler].
     * If this harness was created with a main dispatcher and [effectContext] is left as [EmptyCoroutineContext], the main
     * dispatcher is used as [effectContext].
     *
     * @param effectContext The [CoroutineContext] to use for the [androidx.compose.ui.test.v2.runComposeUiTest] implementation.
     * @param runTestContext The [CoroutineContext] to use for the [androidx.compose.ui.test.v2.runComposeUiTest] implementation.
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

internal fun resolveEffectContext(
    effectContext: CoroutineContext,
    mainDispatcher: TestDispatcher?,
): CoroutineContext = if (effectContext == EmptyCoroutineContext && mainDispatcher != null) {
    mainDispatcher
} else {
    effectContext
}
