package com.fsck.k9.message

import android.content.Intent
import android.net.Uri
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import com.fsck.k9.K9RobolectricTest
import com.fsck.k9.activity.MessageCompose
import com.fsck.k9.ui.compose.IntentData
import com.fsck.k9.ui.compose.IntentDataMapper
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

        assertThat(result.mailToUri).isEqualTo(uri)
        assertThat(result.startedByExternalIntent).isTrue()
        assertThat(result.shouldInitFromSendOrViewIntent).isTrue()
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

        assertThat(result.subject).isEqualTo(subject)
        assertThat(result.extraText).isEqualTo(text)
        assertThat(result.intentType).isEqualTo("text/plain")
        assertThat(result.extraStream).isEqualTo(listOf(streamUri))
        assertThat(result.startedByExternalIntent).isTrue()
        assertThat(result.shouldInitFromSendOrViewIntent).isTrue()
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

        assertThat(result.extraStream.size).isEqualTo(2)
        assertThat(result.extraStream.contains(streamUri1)).isTrue()
        assertThat(result.extraStream.contains(streamUri2)).isTrue()
    }

    @Test
    fun `initFromIntent should extract trustId from ACTION_AUTOCRYPT_PEER`() {
        val trustId = "1234-5678"
        val intent = Intent(MessageCompose.ACTION_AUTOCRYPT_PEER).apply {
            putExtra(OpenPgpApi.EXTRA_AUTOCRYPT_PEER_ID, trustId)
        }

        val result = intentDataMapper.initFromIntent(intent)

        assertThat(result.trustId).isEqualTo(trustId)
        assertThat(result.startedByExternalIntent).isTrue()
    }

    @Test
    fun `initFromIntent should return default IntentData for unsupported action`() {
        val intent = Intent("com.example.UNKNOWN_ACTION")

        val result = intentDataMapper.initFromIntent(intent)

        assertThat(result).isEqualTo(IntentData())
    }
}
