package com.fsck.k9.message

import android.content.Intent
import android.net.Uri
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.ui.compose.IntentData
import com.fsck.k9.ui.compose.IntentDataMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.openintents.openpgp.util.OpenPgpApi

class IntentDataMapperTest : K9RobolectricTest() {

    private lateinit var intentDataMapper: IntentDataMapper

    @Before
    fun setup() {
        intentDataMapper = IntentDataMapper()
    }

    @Test
    fun `initFromIntent should extract mailToUri from ACTION_VIEW`() {
        val uri = Uri.parse("mailto:test@example.com")
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = uri
        }

        val result = intentDataMapper.initFromIntent(intent)

        assertEquals(uri, result.mailToUri)
        assertTrue(result.startedByExternalIntent)
        assertTrue(result.shouldInitFromSendOrViewIntent)
    }

    @Test
    fun `initFromIntent should extract subject and text from ACTION_SEND`() {
        val subject = "Test Subject"
        val text = "Body text"
        val streamUri = Uri.parse("content://attachment")

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_STREAM, streamUri)
        }

        val result = intentDataMapper.initFromIntent(intent)

        assertEquals(subject, result.subject)
        assertEquals(text, result.extraText)
        assertEquals("text/plain", result.intentType)
        assertEquals(listOf(streamUri), result.extraStream)
        assertTrue(result.startedByExternalIntent)
        assertTrue(result.shouldInitFromSendOrViewIntent)
    }

    @Test
    fun `initFromIntent should extract multiple streams from ACTION_SEND_MULTIPLE`() {
        val streamUri1 = Uri.parse("content://attachment/1")
        val streamUri2 = Uri.parse("content://attachment/2")

        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(
                Intent.EXTRA_STREAM,
                arrayListOf(streamUri1, streamUri2),
            )
        }

        val result = intentDataMapper.initFromIntent(intent)

        assertEquals(2, result.extraStream.size)
        assertTrue(result.extraStream.contains(streamUri1))
        assertTrue(result.extraStream.contains(streamUri2))
    }

    @Test
    fun `initFromIntent should extract trustId from ACTION_AUTOCRYPT_PEER`() {
        val trustId = "1234-5678"
        val intent = Intent(MessageCompose.ACTION_AUTOCRYPT_PEER).apply {
            putExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_ID, trustId)
        }

        val result = intentDataMapper.initFromIntent(intent)

        assertEquals(trustId, result.trustId)
        assertTrue(result.startedByExternalIntent)
    }

    @Test
    fun `initFromIntent should return default IntentData for unsupported action`() {
        val intent = Intent("com.example.UNKNOWN_ACTION")

        val result = intentDataMapper.initFromIntent(intent)

        assertEquals(IntentData(), result)
    }
}
