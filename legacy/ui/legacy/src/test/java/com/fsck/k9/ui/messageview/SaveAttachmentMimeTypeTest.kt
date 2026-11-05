package com.fsck.k9.ui.messageview

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.android.testing.RobolectricTest

class SaveAttachmentMimeTypeTest : RobolectricTest() {
    @Test
    fun `non-default MIME type should be kept`() {
        val result = getSaveAttachmentMimeType(displayName = "document.pdf", mimeType = "application/pdf")

        assertThat(result).isEqualTo("application/pdf")
    }

    @Test
    fun `non-default MIME type should be kept even if it does not match the file extension`() {
        val result = getSaveAttachmentMimeType(displayName = "image.png", mimeType = "application/pdf")

        assertThat(result).isEqualTo("application/pdf")
    }

    @Test
    fun `default MIME type should be replaced with MIME type inferred from file extension`() {
        val result = getSaveAttachmentMimeType(displayName = "receipt.pdf", mimeType = "application/octet-stream")

        assertThat(result).isEqualTo("application/pdf")
    }

    @Test
    fun `default MIME type should be matched case-insensitively`() {
        val result = getSaveAttachmentMimeType(displayName = "Receipt.PDF", mimeType = "Application/Octet-Stream")

        assertThat(result).isEqualTo("application/pdf")
    }

    @Test
    fun `default MIME type should be kept for unknown file extension`() {
        val result = getSaveAttachmentMimeType(displayName = "data.unknownext", mimeType = "application/octet-stream")

        assertThat(result).isEqualTo("application/octet-stream")
    }

    @Test
    fun `default MIME type should be kept for file name without extension`() {
        val result = getSaveAttachmentMimeType(displayName = "README", mimeType = "application/octet-stream")

        assertThat(result).isEqualTo("application/octet-stream")
    }

    @Test
    fun `default MIME type should be kept when display name is missing`() {
        val result = getSaveAttachmentMimeType(displayName = null, mimeType = "application/octet-stream")

        assertThat(result).isEqualTo("application/octet-stream")
    }
}
