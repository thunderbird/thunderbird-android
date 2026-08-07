package com.fsck.k9.helper

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MimeTypeUtilTest {

    @Test
    fun `getMimeTypeForFilename returns extension-consistent type for a known extension`() {
        val result = MimeTypeUtil.getMimeTypeForFilename("appointment.ics", "text/plain")

        assertThat(result).isEqualTo("text/calendar")
    }

    @Test
    fun `getMimeTypeForFilename returns extension-consistent type for a pkpass file`() {
        val result = MimeTypeUtil.getMimeTypeForFilename("boardingpass.pkpass", "application/octet-stream")

        assertThat(result).isEqualTo("application/vnd.apple.pkpass")
    }

    @Test
    fun `getMimeTypeForFilename ignores the declared type when the extension is known`() {
        val result = MimeTypeUtil.getMimeTypeForFilename("notes.txt", "application/octet-stream")

        assertThat(result).isEqualTo("text/plain")
    }

    @Test
    fun `getMimeTypeForFilename falls back to declared type when there is no extension`() {
        val result = MimeTypeUtil.getMimeTypeForFilename("noname", "text/plain")

        assertThat(result).isEqualTo("text/plain")
    }

    @Test
    fun `getMimeTypeForFilename falls back to default type when there is no extension and no declared type`() {
        val result = MimeTypeUtil.getMimeTypeForFilename("noname", null)

        assertThat(result).isEqualTo("application/octet-stream")
    }

    @Test
    fun `getMimeTypeForFilename resolves an unknown extension to the default type`() {
        val result = MimeTypeUtil.getMimeTypeForFilename("mystery.unknownext", "text/plain")

        assertThat(result).isEqualTo("application/octet-stream")
    }
}
