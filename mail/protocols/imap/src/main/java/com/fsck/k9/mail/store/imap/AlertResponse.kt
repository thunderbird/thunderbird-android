package com.fsck.k9.mail.store.imap

import com.fsck.k9.mail.store.imap.ImapResponseParser.equalsIgnoreCase

internal object AlertResponse {
    private const val ALERT_RESPONSE_CODE = "ALERT"
    private const val MINIMUM_RESPONSE_SIZE = 3

    @JvmStatic
    fun getAlertText(response: ImapResponse): String? {
        return if (
            response.size >= MINIMUM_RESPONSE_SIZE &&
            response.isList(1)
        ) {
            val responseTextCode = response.getList(1)
            if (responseTextCode.size == 1 && equalsIgnoreCase(responseTextCode[0], ALERT_RESPONSE_CODE)) {
                response.getString(2)
            } else {
                null
            }
        } else {
            null
        }
    }
}
