package com.fsck.k9.provider

import android.net.Uri
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.helper.MimeTypeUtil
import java.io.FileNotFoundException
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test
import org.junit.Assert.assertThrows

class AttachmentProviderTest : RobolectricTest() {
    private val testSubject = AttachmentProvider()

    @Test
    fun getTypeWithInvalidAccountIdReturnsDefaultMimeType() {
        val result = testSubject.getType(Uri.parse("content://attachmentprovider/not-a-uuid/1"))

        assertThat(result).isEqualTo(MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE)
    }

    @Test
    fun queryWithInvalidAccountIdReturnsNull() {
        val result = testSubject.query(Uri.parse("content://attachmentprovider/not-a-uuid/1"), null, null, null, null)

        assertThat(result).isNull()
    }

    @Test
    fun openFileWithInvalidAccountIdThrowsFileNotFound() {
        assertThrows(FileNotFoundException::class.java) {
            testSubject.openFile(Uri.parse("content://attachmentprovider/not-a-uuid/1"), "r")
        }
    }

    @Test
    fun getTypeWithMissingPathReturnsDefaultMimeType() {
        val result = testSubject.getType(Uri.parse("content://attachmentprovider"))

        assertThat(result).isEqualTo(MimeTypeUtil.DEFAULT_ATTACHMENT_MIME_TYPE)
    }
}
