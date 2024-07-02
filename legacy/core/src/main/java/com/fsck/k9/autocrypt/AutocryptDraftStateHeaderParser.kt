package com.fsck.k9.autocrypt

import com.fsck.k9.mail.internet.MimeUtility

class AutocryptDraftStateHeaderParser internal constructor() {

    fun parseAutocryptDraftStateHeader(headerValue: String): AutocryptDraftStateHeader? {
        val parameters = MimeUtility.getAllHeaderParameters(headerValue)

        val isEncryptStr = parameters.remove(AutocryptDraftStateHeader.PARAM_ENCRYPT) ?: return null
        val isEncrypt = isEncryptStr == AutocryptDraftStateHeader.VALUE_YES

        val isSignOnlyStr = parameters.remove(AutocryptDraftStateHeader.PARAM_SIGN_ONLY)
        val isSignOnly = isSignOnlyStr == AutocryptDraftStateHeader.VALUE_YES

        val isReplyStr = parameters.remove(AutocryptDraftStateHeader.PARAM_IS_REPLY)
        val isReply = isReplyStr == AutocryptDraftStateHeader.VALUE_YES

        val isByChoiceStr = parameters.remove(AutocryptDraftStateHeader.PARAM_BY_CHOICE)
        val isByChoice = isByChoiceStr == AutocryptDraftStateHeader.VALUE_YES

        val isPgpInlineStr = parameters.remove(AutocryptDraftStateHeader.PARAM_PGP_INLINE)
        val isPgpInline = isPgpInlineStr == AutocryptDraftStateHeader.VALUE_YES

        if (hasCriticalParameters(parameters)) {
            return null
        }

        return AutocryptDraftStateHeader(isEncrypt, isSignOnly, isReply, isByChoice, isPgpInline, parameters)
    }

    private fun hasCriticalParameters(parameters: Map<String, String>): Boolean {
        for (parameterName in parameters.keys) {
            if (!parameterName.startsWith("_")) {
                return true
            }
        }
        return false
    }
}
