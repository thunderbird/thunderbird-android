package com.fsck.k9.message

import android.net.Uri
import assertk.Assert
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isGreaterThan
import com.fsck.k9.mail.internet.MimeHeaderChecker
import com.fsck.k9.mail.internet.TextBody
import net.thunderbird.core.android.account.Identity
import net.thunderbird.core.android.account.QuoteStyle
import net.thunderbird.core.android.testing.RobolectricTest
import org.junit.Test

private const val IDENTITY_HEADER = "X-K9mail-Identity"

class IdentityHeaderBuilderTest : RobolectricTest() {
    @Test
    fun `valid unstructured header field value`() {
        val signature = "a".repeat(1000)

        val identityHeader = IdentityHeaderBuilder()
            .setCursorPosition(0)
            .setIdentity(createIdentity(signatureUse = true))
            .setIdentityChanged(false)
            .setMessageFormat(SimpleMessageFormat.TEXT)
            .setMessageReference(null)
            .setQuotedHtmlContent(null)
            .setQuoteStyle(QuoteStyle.PREFIX)
            .setQuoteTextMode(QuotedTextMode.NONE)
            .setSignature(signature)
            .setSignatureChanged(true)
            .setBody(TextBody("irrelevant"))
            .setBodyPlain(null)
            .build()

        assertThat(identityHeader.length).isGreaterThan(1000)
        assertIsValidHeader(identityHeader)
    }

    @Test
    fun `identity header without identity name`() {
        val identityHeader = IdentityHeaderBuilder()
            .setIdentity(createIdentity(email = "test@domain.example", name = null))
            .setIdentityChanged(true)
            .setBody(TextBody("irrelevant"))
            .setQuoteStyle(QuoteStyle.PREFIX)
            .setMessageFormat(SimpleMessageFormat.TEXT)
            .setQuoteTextMode(QuotedTextMode.NONE)
            .build()

        assertThat(identityHeader).containsParameter(IdentityField.EMAIL, "test@domain.example")
        assertThat(identityHeader).containsParameter(IdentityField.NAME, "")
    }

    private fun assertIsValidHeader(identityHeader: String) {
        try {
            MimeHeaderChecker.checkHeader(IDENTITY_HEADER, identityHeader)
        } catch (e: Exception) {
            println("$IDENTITY_HEADER: $identityHeader")
            throw e
        }
    }

    private fun createIdentity(
        description: String? = null,
        name: String? = null,
        email: String? = null,
        signature: String? = null,
        signatureUse: Boolean = false,
        replyTo: String? = null,
    ): Identity {
        return Identity(description, name, email, signature, signatureUse, replyTo)
    }
}

private fun Assert<String>.containsParameter(identityField: IdentityField, value: String) = given { actual ->
    assertThat("&${unfold(actual)}&").contains("&${identityField.value()}=${Uri.encode(value)}&")
}

private fun unfold(headerValue: String): String {
    return headerValue.replace(Regex("\r?\n "), "")
}
