package com.fsck.k9.mail.store.imap

internal object ResponseCodeExtractor {
    const val AUTHENTICATION_FAILED: String = "AUTHENTICATIONFAILED"

    @JvmStatic
    fun getResponseCode(response: ImapResponse): String? {
        if (response.size < 2 || !response.isList(1)) {
            return null
        }

        val responseTextCode = response.getList(1)
        return if (responseTextCode.size != 1) null else responseTextCode.getString(0)
    }
}
