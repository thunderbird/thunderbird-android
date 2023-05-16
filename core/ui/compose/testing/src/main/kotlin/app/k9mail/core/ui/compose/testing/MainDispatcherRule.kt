package app.k9mail.core.ui.compose.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher

/**
 * A JUnit rule that swaps the [Dispatchers.Main] dispatcher with a [TestDispatcher] for the duration of the test.
 */
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: org.junit.runner.Description?) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: org.junit.runner.Description?) {
        Dispatchers.resetMain()
    }
}
