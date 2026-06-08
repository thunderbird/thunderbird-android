package net.thunderbird.core.ui.testing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration

/**
 * JVM implementation of [ComposeUiTestHarness].
 *
 * It runs Compose UI tests with JUnit.
 */
@OptIn(ExperimentalTestApi::class)
public actual abstract class ComposeUiTestHarness actual constructor() {

    /**
     * Runs [block] inside `runComposeUiTest`.
     */
    public actual fun runComposeTest(
        effectContext: CoroutineContext,
        runTestContext: CoroutineContext,
        testTimeout: Duration,
        block: suspend ComposeUiTestScope.() -> Unit,
    ) {
        runComposeUiTest(
            effectContext = effectContext,
            runTestContext = runTestContext,
            testTimeout = testTimeout,
        ) {
            JvmComposeUiTestScope(this).block()
        }
    }
}
