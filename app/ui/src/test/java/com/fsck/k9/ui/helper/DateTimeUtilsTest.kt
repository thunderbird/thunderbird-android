package com.fsck.k9.ui.helper

import android.os.SystemClock
import com.fsck.k9.RobolectricTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

@Config(qualifiers = "en")
class DateTimeUtilsTest : RobolectricTest() {

    private val context = RuntimeEnvironment.application.applicationContext

    private val zoneId = "Europe/Berlin"
    private val now = LocalDateTime.parse("2020-05-24T15:42")
    private val nowInMillis = now.toEpochMilli()

    @Before
    fun setUp() {
        TimeZone.setDefault(TimeZone.getTimeZone(zoneId))
        SystemClock.setCurrentTimeMillis(nowInMillis)
    }

    @Test
    fun format_messageDate_asTime_now() {
        val displayDate = formatMessageDate(context, nowInMillis)

        assertEquals("3:42 PM", displayDate)
    }

    @Test
    fun format_messageDate_asTime_earlier_today() {
        val messageDate = now.minusHours(6).toEpochMilli()

        val displayDate = formatMessageDate(context, messageDate)

        assertEquals("9:42 AM", displayDate)
    }

    @Test
    fun format_messageDate_asWeekDay_yesterday() {
        val messageDate = now.minusDays(1).toEpochMilli()

        val displayDate = formatMessageDate(context, messageDate)

        assertEquals("Sat", displayDate)
    }

    @Test
    fun format_messageDate_asWeekday_6_days_ago() {
        val messageDate = now.minusDays(6).minusHours(6).toEpochMilli()

        val displayDate = formatMessageDate(context, messageDate)

        assertEquals("Mon", displayDate)
    }

    @Test
    fun format_messageDate_asDay_6_days_and_22_hours_ago() {
        val messageDate = now.minusDays(7).plusHours(2).toEpochMilli()

        val displayDate = formatMessageDate(context, messageDate)

        assertEquals("May 17", displayDate)
    }

    @Test
    fun format_messageDate_asDay_7_days_and_2_hours_ago() {
        val messageDate = now.minusDays(7).minusHours(2).toEpochMilli()

        val displayDate = formatMessageDate(context, messageDate)

        assertEquals("May 17", displayDate)
    }

    @Test
    fun format_messageDate_asDay_start_of_year() {
        val messageDate = LocalDate.parse("2020-01-01").atStartOfDay().toEpochMilli()

        val displayDate = formatMessageDate(context, messageDate)

        assertEquals("Jan 1", displayDate)
    }

    @Test
    fun format_messageDate_asDate_end_of_last_year() {
        val messageDate = LocalDateTime.parse("2019-12-31T23:59").toEpochMilli()

        val displayDate = formatMessageDate(context, messageDate)

        assertEquals("12/31/2019", displayDate)
    }

    private fun LocalDateTime.toEpochMilli() = this.atZone(ZoneId.of(zoneId)).toInstant().toEpochMilli()

}
