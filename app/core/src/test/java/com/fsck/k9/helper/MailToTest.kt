package com.fsck.k9.helper

import android.net.Uri
import androidx.core.net.toUri
import app.k9mail.core.android.testing.RobolectricTest
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.mail.Address
import org.junit.Test

class MailToTest : RobolectricTest() {

    @Test
    fun `isMailTo() with mailto scheme should return true`() {
        val uri = "mailto:nobody".toUri()

        val result = MailTo.isMailTo(uri)

        assertThat(result).isTrue()
    }

    @Test
    fun `isMailTo() with http scheme should return false`() {
        val uri = "http://example.org/".toUri()

        val result = MailTo.isMailTo(uri)

        assertThat(result).isFalse()
    }

    @Test
    fun `isMailTo() with null argument should return false`() {
        val uri: Uri? = null

        val result = MailTo.isMailTo(uri)

        assertThat(result).isFalse()
    }

    @Test
    fun `parse() with null argument should throw`() {
        assertFailure {
            MailTo.parse(null)
        }.isInstanceOf<NullPointerException>()
            .hasMessage("Argument 'uri' must not be null")
    }

    @Test
    fun `parse() without mailto URI should throw`() {
        val uri = "http://example.org/".toUri()

        assertFailure {
            MailTo.parse(uri)
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Not a mailto scheme")
    }

    @Test
    fun `single To recipient`() {
        val uri = "mailto:test@domain.example".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.to).containsExactly("test@domain.example".toAddress())
    }

    @Test
    fun `multiple To recipients`() {
        val uri = "mailto:test1@domain.example?to=test2@domain.example".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.to).containsExactly(
            "test1@domain.example".toAddress(),
            "test2@domain.example".toAddress(),
        )
    }

    @Test
    fun `single Cc recipient`() {
        val uri = "mailto:test1@domain.example?cc=test3@domain.example".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.cc).containsExactly("test3@domain.example".toAddress())
    }

    @Test
    fun `multiple Cc recipients`() {
        val uri = "mailto:test1@domain.example?cc=test3@domain.example,test4@domain.example".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.cc).containsExactly(
            "test3@domain.example".toAddress(),
            "test4@domain.example".toAddress(),
        )
    }

    @Test
    fun `single Bcc recipient`() {
        val uri = "mailto:?bcc=test3@domain.example".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.bcc).containsExactly("test3@domain.example".toAddress())
    }

    @Test
    fun `multiple Bcc recipients`() {
        val uri = "mailto:?bcc=test3@domain.example&bcc=test4@domain.example".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.bcc).containsExactly(
            "test3@domain.example".toAddress(),
            "test4@domain.example".toAddress(),
        )
    }

    @Test
    fun `mailto URI with subject`() {
        val uri = "mailto:?subject=Hello".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.subject).isEqualTo("Hello")
    }

    @Test
    fun `mailto URI with body and additional parameter following`() {
        val uri = "mailto:?body=Test%20Body&something=else".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.body).isEqualTo("Test Body")
    }

    @Test
    fun `In-Reply-To parameter`() {
        val uri = "mailto:?in-reply-to=%3C7C72B202-73F3@somewhere%3E".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.inReplyTo).isEqualTo("<7C72B202-73F3@somewhere>")
    }

    @Test
    fun `In-Reply-To parameter with multiple message IDs should only return first`() {
        val uri = "mailto:?in-reply-to=%3C7C72B202-73F3@somewhere%3E%3C8A39-1A87CB40C114@somewhereelse%3E".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.inReplyTo).isEqualTo("<7C72B202-73F3@somewhere>")
    }

    @Test
    fun `In-Reply-To example from RFC 6068`() {
        val uri = "mailto:list@example.org?In-Reply-To=%3C3469A91.D10AF4C@example.com%3E".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.inReplyTo).isEqualTo("<3469A91.D10AF4C@example.com>")
    }

    @Test
    fun `invalid In-Reply-To value should return null`() {
        val uri = "mailto:?in-reply-to=7C72B202-73F3somewhere".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.inReplyTo).isNull()
    }

    @Test
    fun `empty In-Reply-To value should return null`() {
        val uri = "mailto:?in-reply-to=".toUri()

        val result = MailTo.parse(uri)

        assertThat(result.inReplyTo).isNull()
    }

    @Test
    fun `mixed case parameter names should be treated case insensitive`() {
        val uri = (
            "mailto:" +
                "?to=to@domain.example" +
                "&CC=cc@domain.example" +
                "&bCC=bcc@domain.example" +
                "&SUBJECT=subject" +
                "&BODY=body" +
                "&IN-REPLY-TO=%3Cmsg@id%3E"
            ).toUri()

        val result = MailTo.parse(uri)

        assertThat(result.to).containsExactly("to@domain.example".toAddress())
        assertThat(result.cc).containsExactly("cc@domain.example".toAddress())
        assertThat(result.bcc).containsExactly("bcc@domain.example".toAddress())
        assertThat(result.subject).isEqualTo("subject")
        assertThat(result.body).isEqualTo("body")
        assertThat(result.inReplyTo).isEqualTo("<msg@id>")
    }
}

private fun String.toAddress(): Address = Address(this)
