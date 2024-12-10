package com.fsck.k9.message.quote

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.fsck.k9.K9
import java.time.ZonedDateTime
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import org.junit.After
import org.junit.Before
import org.junit.Test

class QuoteDateFormatterTest {
    private lateinit var originalLocale: Locale
    private var originalTimeZone: TimeZone? = null
    private val quoteDateFormatter = QuoteDateFormatter()

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("GMT+02:00"))
    }

    @After
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun hideTimeZoneEnabled_UsLocale() {
        K9.isHideTimeZone = true
        Locale.setDefault(Locale.US)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate.normalizeDate()).isEqualTo("September 19, 2020, 8:00:00 PM UTC")
    }

    @Test
    fun hideTimeZoneEnabled_GermanyLocale() {
        K9.isHideTimeZone = true
        Locale.setDefault(Locale.GERMANY)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate.normalizeDate()).isEqualTo("19. September 2020, 20:00:00 UTC")
    }

    @Test
    fun hideTimeZoneDisabled_UsLocale() {
        K9.isHideTimeZone = false
        Locale.setDefault(Locale.US)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate.normalizeDate()).isEqualTo("September 19, 2020, 10:00:00 PM GMT+02:00")
    }

    @Test
    fun hideTimeZoneDisabled_GermanyLocale() {
        K9.isHideTimeZone = false
        Locale.setDefault(Locale.GERMANY)

        val formattedDate = quoteDateFormatter.format("2020-09-19T20:00:00+00:00".toDate())

        assertThat(formattedDate.normalizeDate()).isEqualTo("19. September 2020, 22:00:00 GMT+02:00")
    }

    private fun String.toDate() = Date(ZonedDateTime.parse(this).toEpochSecond() * 1000L)

    // QuoteDateFormatter uses java.text.DateFormat internally. Depending on the JDK/JRE version the output is
    // different. We normalize the output here so the tests don't depend on a specific JDK version.
    private fun String.normalizeDate(): String {
        return this
            .replace(" at", ",")
            .replace(" um", ",")
            .replace("\u202F", " ")
    }
}
