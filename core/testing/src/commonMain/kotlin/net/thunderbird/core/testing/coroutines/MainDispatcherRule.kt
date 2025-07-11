package net.thunderbird.core.testing.coroutines

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

/**
 * A JUnit rule that swaps the [kotlinx.coroutines.Dispatchers.Main] dispatcher with a [kotlinx.coroutines.test.TestDispatcher] for the duration of the test.
 *
 * Use this rule to ensure that coroutines running on the main dispatcher are executed in a controlled manner during tests.
 *
 * This uses [kotlinx.coroutines.test.UnconfinedTestDispatcher] by default, but you can provide a different [kotlinx.coroutines.test.TestDispatcher] if needed.
 * Especially when testing view models use the [kotlinx.coroutines.test.StandardTestDispatcher], this allows you to
 * control the execution of coroutines in a more predictable way.
 *
 * @param testDispatcher The [kotlinx.coroutines.test.TestDispatcher] to use as the main dispatcher during tests. Defaults to [kotlinx.coroutines.test.UnconfinedTestDispatcher].
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}
