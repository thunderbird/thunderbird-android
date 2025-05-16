package net.thunderbird.core.testing

import kotlin.time.Duration
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

class TestClock(
    private var currentTime: Instant = Clock.System.now(),
) : Clock {
    override fun now(): Instant = currentTime

    fun changeTimeTo(time: Instant) {
        currentTime = time
    }

    fun advanceTimeBy(duration: Duration) {
        currentTime += duration
    }
}
