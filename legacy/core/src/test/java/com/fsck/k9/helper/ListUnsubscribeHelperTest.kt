package com.fsck.k9.helper

import androidx.core.net.toUri
import app.k9mail.core.android.testing.RobolectricTest
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.mail.internet.MimeMessage
import org.junit.Test

class ListUnsubscribeHelperTest : RobolectricTest() {
    @Test
    fun `get list unsubscribe url - should accept mailto`() {
        val message = buildMimeMessageWithListUnsubscribeValue(
            "<mailto:unsubscribe@example.com>",
        )
        val result = ListUnsubscribeHelper.getPreferredListUnsubscribeUri(message)
        assertThat(result).isEqualTo(MailtoUnsubscribeUri("mailto:unsubscribe@example.com".toUri()))
    }

    @Test
    fun `get list unsubscribe url - should prefer mailto 1`() {
        val message = buildMimeMessageWithListUnsubscribeValue(
            "<mailto:unsubscribe@example.com>, <https://example.com/unsubscribe>",
        )
        val result = ListUnsubscribeHelper.getPreferredListUnsubscribeUri(message)
        assertThat(result).isEqualTo(MailtoUnsubscribeUri("mailto:unsubscribe@example.com".toUri()))
    }

    @Test
    fun `get list unsubscribe url - should prefer mailto 2`() {
        val message = buildMimeMessageWithListUnsubscribeValue(
            "<https://example.com/unsubscribe>, <mailto:unsubscribe@example.com>",
        )
        val result = ListUnsubscribeHelper.getPreferredListUnsubscribeUri(message)
        assertThat(result).isEqualTo(MailtoUnsubscribeUri("mailto:unsubscribe@example.com".toUri()))
    }

    @Test
    fun `get list unsubscribe url - should allow https if no mailto`() {
        val message = buildMimeMessageWithListUnsubscribeValue(
            "<https://example.com/unsubscribe>",
        )
        val result = ListUnsubscribeHelper.getPreferredListUnsubscribeUri(message)
        assertThat(result).isEqualTo(HttpsUnsubscribeUri("https://example.com/unsubscribe".toUri()))
    }

    @Test
    fun `get list unsubscribe url - should correctly parse uncommon urls`() {
        val message = buildMimeMessageWithListUnsubscribeValue(
            "<https://domain.example/one,two>",
        )
        val result = ListUnsubscribeHelper.getPreferredListUnsubscribeUri(message)
        assertThat(result).isEqualTo(HttpsUnsubscribeUri("https://domain.example/one,two".toUri()))
    }

    @Test
    fun `get list unsubscribe url - should ignore unsafe entries 1`() {
        val message = buildMimeMessageWithListUnsubscribeValue(
            "<http://example.com/unsubscribe>",
        )
        val result = ListUnsubscribeHelper.getPreferredListUnsubscribeUri(message)
        assertThat(result).isNull()
    }

    @Test
    fun `get list unsubscribe url - should ignore unsafe entries 2`() {
        val message = buildMimeMessageWithListUnsubscribeValue(
            "<http://example.com/unsubscribe>, <https://example.com/unsubscribe>",
        )
        val result = ListUnsubscribeHelper.getPreferredListUnsubscribeUri(message)
        assertThat(result).isEqualTo(HttpsUnsubscribeUri("https://example.com/unsubscribe".toUri()))
    }

    @Test
    fun `get list unsubscribe url - should ignore empty`() {
        val message = buildMimeMessageWithListUnsubscribeValue(
            "",
        )
        val result = ListUnsubscribeHelper.getPreferredListUnsubscribeUri(message)
        assertThat(result).isNull()
    }

    @Test
    fun `get list unsubscribe url - should ignore missing header`() {
        val message = MimeMessage()
        val result = ListUnsubscribeHelper.getPreferredListUnsubscribeUri(message)
        assertThat(result).isNull()
    }

    private fun buildMimeMessageWithListUnsubscribeValue(value: String): MimeMessage {
        val message = MimeMessage()
        message.addHeader("List-Unsubscribe", value)
        return message
    }
}
