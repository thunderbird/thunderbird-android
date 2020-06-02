package com.fsck.k9.ui.helper

import com.fsck.k9.RobolectricTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(qualifiers = "en")
class SizeFormatterTest : RobolectricTest() {
    private val sizeFormatter = SizeFormatter(RuntimeEnvironment.application.resources)

    @Test
    fun bytes_lower_bound() {
        assertEquals("0 B", sizeFormatter.formatSize(0))
    }

    @Test
    fun bytes_upper_bound() {
        assertEquals("999 B", sizeFormatter.formatSize(999))
    }

    @Test
    fun kilobytes_lower_bound() {
        assertEquals("1.0 kB", sizeFormatter.formatSize(1000))
    }

    @Test
    fun kilobytes_upper_bound() {
        assertEquals("999.9 kB", sizeFormatter.formatSize(999_949))
    }

    @Test
    fun megabytes_lower_bound() {
        assertEquals("1.0 MB", sizeFormatter.formatSize(999_950))
    }

    @Test
    fun megabytes_upper_bound() {
        assertEquals("999.9 MB", sizeFormatter.formatSize(999_949_999))
    }

    @Test
    fun gigabytes_lower_bound() {
        assertEquals("1.0 GB", sizeFormatter.formatSize(999_950_000))
    }

    @Test
    fun gigabytes_2() {
        assertEquals("2.0 GB", sizeFormatter.formatSize(2_000_000_000L))
    }

    @Test
    fun gigabytes_2000() {
        assertEquals("2000.0 GB", sizeFormatter.formatSize(2_000_000_000_000L))
    }
}
