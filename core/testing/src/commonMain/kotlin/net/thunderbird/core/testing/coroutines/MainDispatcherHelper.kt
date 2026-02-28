package net.thunderbird.core.testing.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Helper class to set the main dispatcher for testing.
 *
 * Example usage:
 *
 * ```
 * val mainDispatcherHelper = MainDispatcherHelper()
 *
 * @BeforeTest
 * fun setUp() {
 *    mainDispatcherHelper.setUp()
 *    // Additional setup code...
 * }
 *
 * @AfterTest
 * fun tearDown() {
 *    mainDispatcherHelper.tearDown()
 *    // Additional teardown code...
 * }
 * ```
 *
 * Use [kotlinx.coroutines.test.UnconfinedTestDispatcher] if you want to execute coroutines immediately without any
 * scheduling, which is useful for simple tests. However, if you need more control over the execution of coroutines
 * (e.g., to test delays, timeouts, or to ensure that certain code runs in a specific order), stay with the default
 *  [kotlinx.coroutines.test.StandardTestDispatcher].
 *
 * @param testDispatcher The dispatcher to set as the main dispatcher. Defaults to [StandardTestDispatcher].
 */
class MainDispatcherHelper(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun tearDown() = Dispatchers.resetMain()
}
