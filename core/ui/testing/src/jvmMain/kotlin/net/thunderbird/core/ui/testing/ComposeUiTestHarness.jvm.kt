package net.thunderbird.core.ui.testing

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest

/**
 * JVM implementation of [ComposeUiTestHarness].
 *
 * It uses JUnit to run the tests.
 */
@OptIn(ExperimentalTestApi::class)
public actual abstract class ComposeUiTestHarness actual constructor() {

    public actual fun runComposeTest(
        block: ComposeUiTestScope.() -> Unit,
    ) {
        runComposeUiTest {
            JvmComposeUiTestScope(this).block()
        }
    }
}
