package com.fsck.k9.ui.helper

import android.os.SystemClock
import com.fsck.k9.RobolectricTest
import com.fsck.k9.TestClock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(qualifiers = "en")
class RelativeDateTimeFormatterTest : RobolectricTest() {

    private val context = RuntimeEnvironment.application.applicationContext
    private val clock = TestClock()
    private val dateTimeFormatter = RelativeDateTimeFormatter(context, clock)

    private val zoneId = "Europe/Berlin"

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
    }

    @Test
    fun inFiveMinutesOnNextDay_shouldReturnDay() {
        setClockTo("2020-05-17T23:58")
        val date = "2020-05-18T00:03".toEpochMillis()

        val displayDate = dateTimeFormatter.formatDate(date)

        assertEquals("May 18", displayDate)
    }

    @Test
    fun oneMinuteAgo_shouldReturnTime() {
        setClockTo("2020-05-17T15:42")
        val date = "2020-05-17T15:41".toEpochMillis()

        val displayDate = dateTimeFormatter.formatDate(date)

        assertEquals("3:41 PM", displayDate)
    }

    @Test
    fun sixHoursAgo_shouldReturnTime() {
        setClockTo("2020-05-17T15:42")
        val date = "2020-05-17T09:42".toEpochMillis()

        val displayDate = dateTimeFormatter.formatDate(date)

        assertEquals("9:42 AM", displayDate)
    }

    @Test
    fun yesterday_shouldReturnWeekday() {
        setClockTo("2020-05-17T15:42")
        val date = "2020-05-16T15:42".toEpochMillis()

        val displayDate = dateTimeFormatter.formatDate(date)

        assertEquals("Sat", displayDate)
    }

    @Test
    fun sixDaysAgo_shouldReturnWeekday() {
        setClockTo("2020-05-17T15:42")
        val date = "2020-05-11T09:42".toEpochMillis()

        val displayDate = dateTimeFormatter.formatDate(date)

        assertEquals("Mon", displayDate)
    }

    @Test
    fun sixDaysAndTwentyHours_shouldReturnDay() {
        setClockTo("2020-05-17T15:42")
        val date = "2020-05-10T17:42".toEpochMillis()

        val displayDate = dateTimeFormatter.formatDate(date)

        assertEquals("May 10", displayDate)
    }

    @Test
    fun sevenDaysAndTwoHours_shouldReturnDay() {
        setClockTo("2020-05-17T15:42")
        val date = "2020-05-10T13:42".toEpochMillis()

        val displayDate = dateTimeFormatter.formatDate(date)

        assertEquals("May 10", displayDate)
    }

    @Test
    fun startOfYear_shouldReturnDay() {
        setClockTo("2020-05-17T15:42")
        val date = LocalDate.parse("2020-01-01").atStartOfDay().toEpochMillis()

        val displayDate = dateTimeFormatter.formatDate(date)

        assertEquals("Jan 1", displayDate)
    }

    @Test
    fun endOfLastYear_shouldReturnDate() {
        setClockTo("2020-05-17T15:42")
        val date = LocalDateTime.parse("2019-12-31T23:59").toEpochMillis()

        val displayDate = dateTimeFormatter.formatDate(date)

        assertEquals("12/31/2019", displayDate)
    }

    private fun setClockTo(time: String) {
        val dateTime = LocalDateTime.parse(time)
        val timeInMillis = dateTime.toEpochMillis()
        SystemClock.setCurrentTimeMillis(timeInMillis) // Is handled by ShadowSystemClock
        clock.time = timeInMillis
    }

    private fun String.toEpochMillis() = LocalDateTime.parse(this).toEpochMillis()

    private fun LocalDateTime.toEpochMillis() = this.atZone(ZoneId.of(zoneId)).toInstant().toEpochMilli()
}
