package net.thunderbird.core.testing.coroutines

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotSameInstanceAs
import kotlin.coroutines.ContinuationInterceptor
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherHelperTest {

    private val testDispatcher = StandardTestDispatcher()
    private val helper = MainDispatcherHelper(testDispatcher)

    @AfterTest
    fun tearDown() {
        helper.tearDown()
    }

    @Test
    fun `setUp should set main dispatcher`() = runTest(testDispatcher) {
        helper.setUp()

        var executed = false
        val job = launch(Dispatchers.Main) {
            executed = true
        }

        assertThat(executed).isEqualTo(false)

        testDispatcher.scheduler.advanceUntilIdle()

        assertThat(executed).isEqualTo(true)
        job.join()
    }

    @Test
    fun `tearDown should reset main dispatcher`() {
        helper.setUp()
        helper.tearDown()

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
            if (after != null) {
                assertThat(after).isNotSameInstanceAs(testDispatcher)
            }
        }
    }
}
