package net.thunderbird.core.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.Instant

internal class TestClockTest {

    @Test
    fun `should return the current time`() {
        val testClock = TestClock(Instant.DISTANT_PAST)

        val currentTime = testClock.now()

        assertThat(currentTime).isEqualTo(Instant.DISTANT_PAST)
    }

    @Test
    fun `should return the changed time`() {
        val testClock = TestClock(Instant.DISTANT_PAST)
        testClock.changeTimeTo(Instant.DISTANT_FUTURE)

        val currentTime = testClock.now()

        assertThat(currentTime).isEqualTo(Instant.DISTANT_FUTURE)
    }

    @Test
    fun `should advance time by duration`() {
        val testClock = TestClock(Instant.DISTANT_PAST)
        testClock.advanceTimeBy(1L.milliseconds)

        val currentTime = testClock.now()

        assertThat(currentTime).isEqualTo(Instant.DISTANT_PAST + 1L.milliseconds)
    }
}
