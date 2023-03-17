package com.fsck.k9.mail.store.imap

/**
 * Extracts the response text from a (negative) status response.
 */
internal object ResponseTextExtractor {
    private const val MINIMUM_RESPONSE_SIZE = 2
    private const val RESPONSE_CODE_INDEX = 1
    private const val SIMPLE_RESPONSE_TEXT_INDEX = 1
    private const val EXTENDED_RESPONSE_TEXT_INDEX = 2

    fun getResponseText(response: ImapResponse): String? {
        if (response.size < MINIMUM_RESPONSE_SIZE) return null

        val responseTextIndex = if (response.isList(RESPONSE_CODE_INDEX)) {
            EXTENDED_RESPONSE_TEXT_INDEX
        } else {
            SIMPLE_RESPONSE_TEXT_INDEX
        }

        return if (response.isString(responseTextIndex)) {
            response.getString(responseTextIndex)
        } else {
            null
        }
    }
}
