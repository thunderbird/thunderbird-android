package net.thunderbird.core.ui.contract.udf

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.testing.coroutines.MainDispatcherHelper

class BaseUnidirectionalSliceTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    private val mainDispatcher = MainDispatcherHelper(UnconfinedTestDispatcher())

    @BeforeTest
    fun setUp() {
        mainDispatcher.setUp()
    }

    @AfterTest
    fun tearDown() {
        mainDispatcher.tearDown()
    }

    @Test
    fun `should emit initial state`() = runTest {
        val testSubject = TestBaseSlice(scope = this)

        assertThat(testSubject.state.value).isEqualTo("Initial state")
    }

    @Test
    fun `should update state`() = runTest {
        val testSubject = TestBaseSlice(scope = this)

        testSubject.event("Test event")

        assertThat(testSubject.state.value).isEqualTo("Test event")

        testSubject.event("Another test event")

        assertThat(testSubject.state.value).isEqualTo("Another test event")
    }

    @Test
    fun `should emit effects`() = runTest {
        val testSubject = TestBaseSlice(scope = this)

        testSubject.effect.test {
            testSubject.effect("Test effect")

            assertThat(awaitItem()).isEqualTo("Test effect")

            testSubject.effect("Another test effect")

            assertThat(awaitItem()).isEqualTo("Another test effect")
        }
    }

    private class TestBaseSlice(
        scope: CoroutineScope,
    ) : BaseUnidirectionalSlice<String, String, String>(
        scope = scope,
        initialState = "Initial state",
    ) {
        fun effect(effect: String) {
            emitEffect(effect)
        }

        override fun event(event: String) {
            updateState { event }
        }
    }
}
