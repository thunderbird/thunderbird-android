package com.fsck.k9.ui.helper

import android.os.SystemClock
import com.fsck.k9.Clock
import com.fsck.k9.RobolectricTest
import com.nhaarman.mockitokotlin2.whenever
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(qualifiers = "en")
class RelativeDateTimeFormatterTest : RobolectricTest() {

    private val context = RuntimeEnvironment.application.applicationContext

    private val clock = Mockito.mock(Clock::class.java)
    private val zoneId = "Europe/Berlin"
    private val now = LocalDateTime.parse("2020-05-17T15:42")
    private val nowInMillis = now.toEpochMilli()

    private val dateTimeFormatter = RelativeDateTimeFormatter(context, clock)

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
        SystemClock.setCurrentTimeMillis(nowInMillis) // Is handled by ShadowSystemClock
        whenever(clock.time).thenReturn(nowInMillis)
    }

    @Test
    fun formatMessageDate_asTime_now() {
        val displayDate = dateTimeFormatter.formatMessageDate(nowInMillis)

        assertEquals("3:42 PM", displayDate)
    }

    @Test
    fun formatMessageDate_asTime_earlier_today() {
        val messageDate = now.minusHours(6).toEpochMilli()

        val displayDate = dateTimeFormatter.formatMessageDate(messageDate)

        assertEquals("9:42 AM", displayDate)
    }

    @Test
    fun formatMessageDate_asWeekDay_yesterday() {
        val messageDate = now.minusDays(1).toEpochMilli()

        val displayDate = dateTimeFormatter.formatMessageDate(messageDate)

        assertEquals("Sat", displayDate)
    }

    @Test
    fun formatMessageDate_asWeekday_6_days_ago() {
        val messageDate = now.minusDays(6).minusHours(6).toEpochMilli()

        val displayDate = dateTimeFormatter.formatMessageDate(messageDate)

        assertEquals("Mon", displayDate)
    }

    @Test
    fun formatMessageDate_asDay_6_days_and_22_hours_ago() {
        val messageDate = now.minusDays(7).plusHours(2).toEpochMilli()

        val displayDate = dateTimeFormatter.formatMessageDate(messageDate)

        assertEquals("May 10", displayDate)
    }

    @Test
    fun formatMessageDate_asDay_7_days_and_2_hours_ago() {
        val messageDate = now.minusDays(7).minusHours(2).toEpochMilli()

        val displayDate = dateTimeFormatter.formatMessageDate(messageDate)

        assertEquals("May 10", displayDate)
    }

    @Test
    fun formatMessageDate_asDay_start_of_year() {
        val messageDate = LocalDate.parse("2020-01-01").atStartOfDay().toEpochMilli()

        val displayDate = dateTimeFormatter.formatMessageDate(messageDate)

        assertEquals("Jan 1", displayDate)
    }

    @Test
    fun formatMessageDate_asDate_end_of_last_year() {
        val messageDate = LocalDateTime.parse("2019-12-31T23:59").toEpochMilli()

        val displayDate = dateTimeFormatter.formatMessageDate(messageDate)

        assertEquals("12/31/2019", displayDate)
    }

    private fun LocalDateTime.toEpochMilli() = this.atZone(ZoneId.of(zoneId)).toInstant().toEpochMilli()
}
