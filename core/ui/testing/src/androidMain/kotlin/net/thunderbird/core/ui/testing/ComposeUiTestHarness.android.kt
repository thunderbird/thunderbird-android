package net.thunderbird.core.ui.testing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Android implementation of [ComposeUiTestHarness].
 *
 * It uses Robolectric to run the tests and pro.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
public actual abstract class ComposeUiTestHarness actual constructor() {

    public actual fun runComposeTest(
        block: ComposeUiTestScope.() -> Unit,
    ) {
        runComposeUiTest {
            AndroidComposeUiTestScope(this).block()
        }
    }
}
