package com.fsck.k9.autocrypt

import com.fsck.k9.message.CryptoStatus

data class AutocryptDraftStateHeader(
    val isEncrypt: Boolean,
    val isSignOnly: Boolean,
    val isReply: Boolean,
    val isByChoice: Boolean,
    val isPgpInline: Boolean,
    val parameters: Map<String, String> = mapOf(),
) {

    fun toHeaderValue(): String {
        val builder = StringBuilder()

        builder.append(AutocryptDraftStateHeader.PARAM_ENCRYPT)
        builder.append(if (isEncrypt) "=yes; " else "=no; ")

        if (isReply) {
            builder.append(AutocryptDraftStateHeader.PARAM_IS_REPLY).append("=yes; ")
        }
        if (isSignOnly) {
            builder.append(AutocryptDraftStateHeader.PARAM_SIGN_ONLY).append("=yes; ")
        }
        if (isByChoice) {
            builder.append(AutocryptDraftStateHeader.PARAM_BY_CHOICE).append("=yes; ")
        }
        if (isPgpInline) {
            builder.append(AutocryptDraftStateHeader.PARAM_PGP_INLINE).append("=yes; ")
        }

        return builder.toString()
    }

    companion object {
        const val AUTOCRYPT_DRAFT_STATE_HEADER = "Autocrypt-Draft-State"

        const val PARAM_ENCRYPT = "encrypt"

        const val PARAM_IS_REPLY = "_is-reply-to-encrypted"
        const val PARAM_BY_CHOICE = "_by-choice"
        const val PARAM_PGP_INLINE = "_pgp-inline"
        const val PARAM_SIGN_ONLY = "_sign-only"

        const val VALUE_YES = "yes"

        @JvmStatic
        fun fromCryptoStatus(cryptoStatus: CryptoStatus): AutocryptDraftStateHeader {
            if (cryptoStatus.isSignOnly) {
                return AutocryptDraftStateHeader(
                    false,
                    true,
                    cryptoStatus.isReplyToEncrypted,
                    cryptoStatus.isUserChoice(),
                    cryptoStatus.isPgpInlineModeEnabled,
                    mapOf(),
                )
            }
            return AutocryptDraftStateHeader(
                cryptoStatus.isEncryptionEnabled,
                false,
                cryptoStatus.isReplyToEncrypted,
                cryptoStatus.isUserChoice(),
                cryptoStatus.isPgpInlineModeEnabled,
                mapOf(),
            )
        }
    }
}
