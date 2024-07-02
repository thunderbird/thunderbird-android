package com.fsck.k9

import app.k9mail.core.testing.TestClock
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import java.util.Calendar
import kotlinx.datetime.Instant
import org.junit.Test

class QuietTimeCheckerTest {
    private val clock = TestClock()

    @Test
    fun endTimeBeforeStartTime_timeIsBeforeEndOfQuietTime() {
        setClockTo("02:00")
        val quietTimeChecker = QuietTimeChecker(clock, "22:30", "06:45")

        assertThat(quietTimeChecker.isQuietTime).isTrue()
    }

    @Test
    fun endTimeBeforeStartTime_timeIsAfterEndOfQuietTime() {
        setClockTo("10:00")
        val quietTimeChecker = QuietTimeChecker(clock, "22:30", "06:45")

        assertThat(quietTimeChecker.isQuietTime).isFalse()
    }

    @Test
    fun endTimeBeforeStartTime_timeIsAfterStartOfQuietTime() {
        setClockTo("23:00")
        val quietTimeChecker = QuietTimeChecker(clock, "22:30", "06:45")

        assertThat(quietTimeChecker.isQuietTime).isTrue()
    }

    @Test
    fun endTimeBeforeStartTime_timeIsStartOfQuietTime() {
        setClockTo("22:30")
        val quietTimeChecker = QuietTimeChecker(clock, "22:30", "06:45")

        assertThat(quietTimeChecker.isQuietTime).isTrue()
    }

    @Test
    fun endTimeBeforeStartTime_timeIsEndOfQuietTime() {
        setClockTo("06:45")
        val quietTimeChecker = QuietTimeChecker(clock, "22:30", "06:45")

        assertThat(quietTimeChecker.isQuietTime).isTrue()
    }

    @Test
    fun startTimeBeforeEndTime_timeIsBeforeStartOfQuietTime() {
        setClockTo("02:00")
        val quietTimeChecker = QuietTimeChecker(clock, "09:00", "17:00")

        assertThat(quietTimeChecker.isQuietTime).isFalse()
    }

    @Test
    fun startTimeBeforeEndTime_timeIsAfterStartOfQuietTime() {
        setClockTo("10:00")
        val quietTimeChecker = QuietTimeChecker(clock, "09:00", "17:00")

        assertThat(quietTimeChecker.isQuietTime).isTrue()
    }

    @Test
    fun startTimeBeforeEndTime_timeIsAfterEndOfQuietTime() {
        setClockTo("20:00")
        val quietTimeChecker = QuietTimeChecker(clock, "09:00", "17:00")

        assertThat(quietTimeChecker.isQuietTime).isFalse()
    }

    @Test
    fun startTimeBeforeEndTime_timeIsStartOfQuietTime() {
        setClockTo("09:00")
        val quietTimeChecker = QuietTimeChecker(clock, "09:00", "17:00")

        assertThat(quietTimeChecker.isQuietTime).isTrue()
    }

    @Test
    fun startTimeBeforeEndTime_timeIsEndOfQuietTime() {
        setClockTo("17:00")
        val quietTimeChecker = QuietTimeChecker(clock, "09:00", "17:00")

        assertThat(quietTimeChecker.isQuietTime).isTrue()
    }

    @Test
    fun startTimeEqualsEndTime_timeIsDifferentFromStartAndEndOfQuietTime_shouldReturnFalse() {
        setClockTo("10:00")
        val quietTimeChecker = QuietTimeChecker(clock, "06:00", "06:00")

        assertThat(quietTimeChecker.isQuietTime).isFalse()
    }

    @Test
    fun startTimeEqualsEndTime_timeIsEqualToStartAndEndOfQuietTime_shouldReturnFalse() {
        setClockTo("06:00")
        val quietTimeChecker = QuietTimeChecker(clock, "06:00", "06:00")

        assertThat(quietTimeChecker.isQuietTime).isFalse()
    }

    private fun setClockTo(time: String) {
        val (hourOfDay, minute) = time.split(':').map { it.toInt() }

        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
        calendar.set(Calendar.MINUTE, minute)

        clock.changeTimeTo(Instant.fromEpochMilliseconds(calendar.timeInMillis))
    }
}
