package net.thunderbird.core.ui.testing

/**
 * Platform-agnostic test harness for Compose UI tests.
 *
 * It wraps the platform-specific implementation of the test harness to allow for consistent testing across platforms.
 */
public expect abstract class ComposeUiTestHarness() {

    /**
     * Run a compose UI test harness with the provided block.
     *
     * @param block The block of code to execute within the Compose UI test harness.
     */
    public fun runComposeTest(
        block: ComposeUiTestScope.() -> Unit,
    )
}
