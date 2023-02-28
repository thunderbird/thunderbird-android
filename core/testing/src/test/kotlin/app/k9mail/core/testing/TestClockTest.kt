package app.k9mail.core.testing

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlinx.datetime.Instant
import kotlin.test.Test

internal class TestClockTest {

    @Test
    fun `should return the current time`() {
        val testClock = TestClock()

        val currentTime = testClock.now()

        assertThat(currentTime).isNotNull()
    }

    @Test
    fun `should return the changed time`() {
        val testClock = TestClock()

        testClock.changeTimeTo(Instant.DISTANT_FUTURE)

        val currentTime = testClock.now()

        assertThat(currentTime).isEqualTo(Instant.DISTANT_FUTURE)
    }
}
