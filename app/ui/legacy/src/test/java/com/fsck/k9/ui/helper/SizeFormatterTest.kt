package com.fsck.k9.ui.helper

import app.k9mail.core.android.testing.RobolectricTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@Config(qualifiers = "en")
class SizeFormatterTest : RobolectricTest() {
    private val sizeFormatter = SizeFormatter(RuntimeEnvironment.getApplication().resources)

    @Test
    fun bytes_lower_bound() {
        assertThat(sizeFormatter.formatSize(0)).isEqualTo("0 B")
    }

    @Test
    fun bytes_upper_bound() {
        assertThat(sizeFormatter.formatSize(999)).isEqualTo("999 B")
    }

    @Test
    fun kilobytes_lower_bound() {
        assertThat(sizeFormatter.formatSize(1000)).isEqualTo("1.0 kB")
    }

    @Test
    fun kilobytes_upper_bound() {
        assertThat(sizeFormatter.formatSize(999_949)).isEqualTo("999.9 kB")
    }

    @Test
    fun megabytes_lower_bound() {
        assertThat(sizeFormatter.formatSize(999_950)).isEqualTo("1.0 MB")
    }

    @Test
    fun megabytes_upper_bound() {
        assertThat(sizeFormatter.formatSize(999_949_999)).isEqualTo("999.9 MB")
    }

    @Test
    fun gigabytes_lower_bound() {
        assertThat(sizeFormatter.formatSize(999_950_000)).isEqualTo("1.0 GB")
    }

    @Test
    fun gigabytes_2() {
        assertThat(sizeFormatter.formatSize(2_000_000_000L)).isEqualTo("2.0 GB")
    }

    @Test
    fun gigabytes_2000() {
        assertThat(sizeFormatter.formatSize(2_000_000_000_000L)).isEqualTo("2000.0 GB")
    }
}
