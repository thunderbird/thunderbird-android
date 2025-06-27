package net.thunderbird.core.testing

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
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
