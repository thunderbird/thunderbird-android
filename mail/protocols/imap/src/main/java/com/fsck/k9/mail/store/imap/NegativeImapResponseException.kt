package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.MessagingException

internal class NegativeImapResponseException(
    message: String?,
    private val responses: List<ImapResponse>,
) : MessagingException(message, true) {

    init {
        require(responses.isNotEmpty()) { "List of responses must not be empty" }
    }

    val lastResponse: ImapResponse
        get() = responses.last()

    val responseText: String? by lazy { ResponseTextExtractor.getResponseText(lastResponse) }

    val alertText: String? by lazy { AlertResponse.getAlertText(lastResponse) }

    fun wasByeResponseReceived(): Boolean {
        return responses.any { it.isByeResponse }
    }

    private val ImapResponse.isByeResponse: Boolean
        get() = !isTagged && isNotEmpty() && ImapResponseParser.equalsIgnoreCase(first(), Responses.BYE)
}
