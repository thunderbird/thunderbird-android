package com.fsck.k9.mail.store.imap

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.fsck.k9.mail.store.imap.ImapResponseHelper.createImapResponse
import org.junit.Test

class ResponseTextExtractorTest {
    @Test
    fun `response with response code and response text`() {
        val imapResponse: ImapResponse = createImapResponse("x NO [AUTHENTICATIONFAILED] Authentication error #23")

        val result = ResponseTextExtractor.getResponseText(imapResponse)

        assertThat(result).isEqualTo("Authentication error #23")
    }

    @Test
    fun `response with only response text`() {
        val imapResponse: ImapResponse = createImapResponse("x NO AUTHENTICATE failed")

        val result = ResponseTextExtractor.getResponseText(imapResponse)

        assertThat(result).isEqualTo("AUTHENTICATE failed")
    }

    @Test
    fun `response without response code or text`() {
        val imapResponse: ImapResponse = createImapResponse("x NO")

        val result = ResponseTextExtractor.getResponseText(imapResponse)

        assertThat(result).isNull()
    }

    @Test
    fun `response with only a response code`() {
        val imapResponse: ImapResponse = createImapResponse("x NO [AUTHENTICATIONFAILED]")

        val result = ResponseTextExtractor.getResponseText(imapResponse)

        assertThat(result).isNull()
    }
}
