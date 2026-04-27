package net.thunderbird.core.ui.contract.mvi

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.testing.coroutines.MainDispatcherHelper

class BaseViewModelTest {

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
        val testSubject = TestBaseViewModel()

        assertThat(testSubject.state.value).isEqualTo("Initial state")
    }

    @Test
    fun `should update state`() = runTest {
        val testSubject = TestBaseViewModel()

        testSubject.event("Test event")

        assertThat(testSubject.state.value).isEqualTo("Test event")

        testSubject.event("Another test event")

        assertThat(testSubject.state.value).isEqualTo("Another test event")
    }

    @Test
    fun `should emit effects`() = runTest {
        val testSubject = TestBaseViewModel()

        testSubject.effect.test {
            testSubject.effect("Test effect")

            assertThat(awaitItem()).isEqualTo("Test effect")

            testSubject.effect("Another test effect")

            assertThat(awaitItem()).isEqualTo("Another test effect")
        }
    }

    @Test
    fun `handleOneTimeEvent() should execute block`() = runTest {
        val testSubject = TestBaseViewModel()
        var eventHandled = false

        testSubject.callHandleOneTimeEvent(event = "event") {
            eventHandled = true
        }

        assertThat(eventHandled).isTrue()
    }

    @Test
    fun `handleOneTimeEvent() should execute block only once`() = runTest {
        val testSubject = TestBaseViewModel()
        var eventHandledCount = 0

        repeat(2) {
            testSubject.callHandleOneTimeEvent(event = "event") {
                eventHandledCount++
            }
        }

        assertThat(eventHandledCount).isEqualTo(1)
    }

    @Test
    fun `handleOneTimeEvent() should support multiple one-time events`() = runTest {
        val testSubject = TestBaseViewModel()
        var eventOneHandled = false
        var eventTwoHandled = false

        testSubject.callHandleOneTimeEvent(event = "eventOne") {
            eventOneHandled = true
        }

        assertThat(eventOneHandled).isTrue()
        assertThat(eventTwoHandled).isFalse()

        testSubject.callHandleOneTimeEvent(event = "eventTwo") {
            eventTwoHandled = true
        }

        assertThat(eventOneHandled).isTrue()
        assertThat(eventTwoHandled).isTrue()
    }

    private class TestBaseViewModel : BaseViewModel<String, String, String>("Initial state") {
        override fun event(event: String) {
            updateState { event }
        }

        fun callHandleOneTimeEvent(event: String, block: () -> Unit) {
            handleOneTimeEvent(event, block)
        }

        fun effect(effect: String) {
            emitEffect(effect)
        }
    }
}
