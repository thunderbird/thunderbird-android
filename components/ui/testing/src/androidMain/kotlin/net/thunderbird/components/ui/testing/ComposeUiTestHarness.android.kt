package net.thunderbird.components.ui.testing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlinx.coroutines.test.TestDispatcher
import net.thunderbird.components.ui.testing.coroutines.MainDispatcherHelper
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Android implementation of [ComposeUiTestHarness].
 *
 * It runs Compose UI tests with Robolectric.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
public actual abstract class ComposeUiTestHarness actual constructor(
    private val mainDispatcher: TestDispatcher?,
) {

    /**
     * Runs [block] inside `runComposeUiTest`.
     */
    public actual fun runComposeTest(
        effectContext: CoroutineContext,
        runTestContext: CoroutineContext,
        testTimeout: Duration,
        block: suspend ComposeUiTestScope.() -> Unit,
    ) {
        val mainDispatcherHelper = mainDispatcher?.let(::MainDispatcherHelper)
        val resolvedEffectContext = resolveEffectContext(effectContext, mainDispatcher)

        try {
            mainDispatcherHelper?.setUp()
            runComposeUiTest(
                effectContext = resolvedEffectContext,
                runTestContext = runTestContext,
                testTimeout = testTimeout,
            ) {
                AndroidComposeUiTestScope(this).block()
            }
        } finally {
            mainDispatcherHelper?.tearDown()
        }
    }
}
