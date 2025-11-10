package net.thunderbird.core.testing.coroutines

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isTrue
import kotlin.coroutines.ContinuationInterceptor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Rule
import org.junit.Test
import org.junit.runner.Description
import org.junit.runners.model.Statement

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRuleTest {

    @get:Rule
    val rule = MainDispatcherRule(StandardTestDispatcher())

    @Test
    fun `given rule with StandardTestDispatcher when posting to Main then it is controlled by provided scheduler`() =
        runTest(rule.testDispatcher) {
            var executed = false

            launch(Dispatchers.Main) {
                delay(1000)
                executed = true
            }

            repeat(9) {
                // wait a bit for the coroutine to start this delays by 900ms total
                if (executed) return@repeat
                delay(100)
            }

            // Should not run until time advances on the provided scheduler
            assertThat(executed).isFalse()

            rule.testDispatcher.scheduler.advanceTimeBy(1000)
            rule.testDispatcher.scheduler.advanceUntilIdle()

            assertThat(executed).isTrue()
        }

    @Test
    fun `given rule when applied around statement then sets Main and resets after evaluation`() {
        // Arrange
        val testDispatcher = StandardTestDispatcher()
        val localRule = MainDispatcherRule(testDispatcher)

        val inner = object : Statement() {
            override fun evaluate() {
                runTest(testDispatcher) {
                    withContext(Dispatchers.Main) { /* no-op */ }
                }
            }
        }

        // Act
        val wrapped = localRule.apply(inner, Description.EMPTY)
        wrapped.evaluate()

        // Assert
        val outcome = runCatching {
            var after: CoroutineDispatcher? = null
            runTest {
                withContext(Dispatchers.Main) {
                    after = coroutineContext[ContinuationInterceptor] as CoroutineDispatcher
                }
            }
            after
        }
        outcome.onSuccess { after ->
            assertThat(after).isNotSameInstanceAs(testDispatcher)
        }
    }
}
