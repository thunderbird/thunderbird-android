package com.fsck.k9.view

import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import app.k9mail.core.ui.legacy.designsystem.atom.icon.Icons
import com.fsck.k9.mailstore.CryptoResultAnnotation
import com.fsck.k9.mailstore.CryptoResultAnnotation.CryptoError
import com.fsck.k9.ui.R
import org.openintents.openpgp.OpenPgpDecryptionResult.RESULT_ENCRYPTED
import org.openintents.openpgp.OpenPgpDecryptionResult.RESULT_INSECURE
import org.openintents.openpgp.OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED
import org.openintents.openpgp.OpenPgpSignatureResult
import org.openintents.openpgp.OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED
import org.openintents.openpgp.OpenPgpSignatureResult.RESULT_INVALID_KEY_INSECURE
import org.openintents.openpgp.OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED
import org.openintents.openpgp.OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE
import org.openintents.openpgp.OpenPgpSignatureResult.RESULT_KEY_MISSING
import org.openintents.openpgp.OpenPgpSignatureResult.RESULT_NO_SIGNATURE
import org.openintents.openpgp.OpenPgpSignatureResult.RESULT_VALID_KEY_CONFIRMED
import org.openintents.openpgp.OpenPgpSignatureResult.RESULT_VALID_KEY_UNCONFIRMED
import org.openintents.openpgp.OpenPgpSignatureResult.SenderStatusResult
import org.openintents.openpgp.OpenPgpSignatureResult.SenderStatusResult.UNKNOWN
import org.openintents.openpgp.OpenPgpSignatureResult.SenderStatusResult.USER_ID_CONFIRMED
import org.openintents.openpgp.OpenPgpSignatureResult.SenderStatusResult.USER_ID_MISSING
import org.openintents.openpgp.OpenPgpSignatureResult.SenderStatusResult.USER_ID_UNCONFIRMED

@Suppress("detekt.UnnecessaryAnnotationUseSiteTarget") // https://github.com/detekt/detekt/issues/8212
enum class MessageCryptoDisplayStatus(
    val isEnabled: Boolean = true,

    @param:AttrRes
    val colorAttr: Int,

    @param:DrawableRes
    val statusIconRes: Int,

    @param:StringRes
    val titleTextRes: Int? = null,

    @param:StringRes
    val descriptionTextRes: Int? = null,
) {
    LOADING(
        isEnabled = false,
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = Icons.Outlined.NoEncryption,
    ),
    CANCELLED(
        colorAttr = R.attr.openpgp_black,
        statusIconRes = Icons.Outlined.Help,
        titleTextRes = R.string.crypto_msg_title_encrypted_unknown,
        descriptionTextRes = R.string.crypto_msg_cancelled,
    ),
    DISABLED(
        isEnabled = false,
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = Icons.Outlined.NoEncryption,
        titleTextRes = R.string.crypto_msg_title_plaintext,
    ),
    UNENCRYPTED_SIGN_ERROR(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = Icons.Outlined.Help,
        titleTextRes = R.string.crypto_msg_title_plaintext,
        descriptionTextRes = R.string.crypto_msg_unencrypted_sign_error,
    ),
    INCOMPLETE_SIGNED(
        colorAttr = R.attr.openpgp_black,
        statusIconRes = Icons.Outlined.Help,
        titleTextRes = R.string.crypto_msg_title_plaintext,
        descriptionTextRes = R.string.crypto_msg_incomplete_signed,
    ),
    UNENCRYPTED_SIGN_VERIFIED(
        colorAttr = R.attr.openpgp_blue,
        statusIconRes = R.drawable.status_signature_dots_3,
        titleTextRes = R.string.crypto_msg_title_unencrypted_signed_e2e,
        descriptionTextRes = R.string.crypto_msg_unencrypted_sign_verified,
    ),
    UNENCRYPTED_SIGN_UNVERIFIED(
        colorAttr = R.attr.openpgp_blue,
        statusIconRes = Icons.Outlined.CheckCircle,
        titleTextRes = R.string.crypto_msg_title_unencrypted_signed_e2e,
    ),
    UNENCRYPTED_SIGN_UNKNOWN(
        colorAttr = R.attr.openpgp_orange,
        statusIconRes = Icons.Outlined.Help,
        titleTextRes = R.string.crypto_msg_title_unencrypted_signed,
        descriptionTextRes = R.string.crypto_msg_unencrypted_sign_unknown,
    ),
    UNENCRYPTED_SIGN_MISMATCH(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = Icons.Outlined.Help,
        titleTextRes = R.string.crypto_msg_title_unencrypted_signed,
        descriptionTextRes = R.string.crypto_msg_unencrypted_sign_mismatch,
    ),
    UNENCRYPTED_SIGN_EXPIRED(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = Icons.Outlined.Help,
        titleTextRes = R.string.crypto_msg_title_unencrypted_signed,
        descriptionTextRes = R.string.crypto_msg_unencrypted_sign_expired,
    ),
    UNENCRYPTED_SIGN_REVOKED(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = Icons.Outlined.Help,
        titleTextRes = R.string.crypto_msg_title_unencrypted_signed,
        descriptionTextRes = R.string.crypto_msg_unencrypted_sign_revoked,
    ),
    UNENCRYPTED_SIGN_INSECURE(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = Icons.Outlined.Help,
        titleTextRes = R.string.crypto_msg_title_unencrypted_signed,
        descriptionTextRes = R.string.crypto_msg_unencrypted_sign_insecure,
    ),
    ENCRYPTED_SIGN_VERIFIED(
        colorAttr = R.attr.openpgp_green,
        statusIconRes = R.drawable.status_lock_dots_3,
        titleTextRes = R.string.crypto_msg_title_encrypted_signed_e2e,
        descriptionTextRes = R.string.crypto_msg_encrypted_sign_verified,
    ),
    ENCRYPTED_SIGN_UNVERIFIED(
        colorAttr = R.attr.openpgp_green,
        statusIconRes = Icons.Outlined.Lock,
        titleTextRes = R.string.crypto_msg_title_encrypted_signed_e2e,
    ),
    ENCRYPTED_SIGN_UNKNOWN(
        colorAttr = R.attr.openpgp_orange,
        statusIconRes = Icons.Outlined.Help,
        titleTextRes = R.string.crypto_msg_title_encrypted_signed,
        descriptionTextRes = R.string.crypto_msg_encrypted_sign_unknown,
    ),
    ENCRYPTED_SIGN_MISMATCH(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = R.drawable.status_lock_error,
        titleTextRes = R.string.crypto_msg_title_encrypted_signed,
        descriptionTextRes = R.string.crypto_msg_encrypted_sign_mismatch,
    ),
    ENCRYPTED_SIGN_EXPIRED(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = R.drawable.status_lock_error,
        titleTextRes = R.string.crypto_msg_title_encrypted_signed,
        descriptionTextRes = R.string.crypto_msg_encrypted_sign_expired,
    ),
    ENCRYPTED_SIGN_REVOKED(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = R.drawable.status_lock_error,
        titleTextRes = R.string.crypto_msg_title_encrypted_signed,
        descriptionTextRes = R.string.crypto_msg_encrypted_sign_revoked,
    ),
    ENCRYPTED_SIGN_INSECURE(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = R.drawable.status_lock_error,
        titleTextRes = R.string.crypto_msg_title_encrypted_signed,
        descriptionTextRes = R.string.crypto_msg_encrypted_sign_insecure,
    ),
    ENCRYPTED_SIGN_ERROR(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = R.drawable.status_lock_error,
        titleTextRes = R.string.crypto_msg_title_encrypted_signed,
        descriptionTextRes = R.string.crypto_msg_encrypted_sign_error,
    ),
    ENCRYPTED_INSECURE(
        colorAttr = R.attr.openpgp_red,
        statusIconRes = R.drawable.status_lock_error,
        titleTextRes = R.string.crypto_msg_title_encrypted_signed,
        descriptionTextRes = R.string.crypto_msg_encrypted_insecure,
    ),
    ENCRYPTED_UNSIGNED(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = R.drawable.status_lock_error,
        titleTextRes = R.string.crypto_msg_title_encrypted_unsigned,
        descriptionTextRes = R.string.crypto_msg_encrypted_unsigned,
    ),
    ENCRYPTED_ERROR(
        colorAttr = R.attr.openpgp_red,
        statusIconRes = R.drawable.status_lock_error,
        titleTextRes = R.string.crypto_msg_title_encrypted_unknown,
        descriptionTextRes = R.string.crypto_msg_encrypted_error,
    ),
    INCOMPLETE_ENCRYPTED(
        colorAttr = R.attr.openpgp_black,
        statusIconRes = Icons.Outlined.Help,
        titleTextRes = R.string.crypto_msg_title_encrypted_unknown,
        descriptionTextRes = R.string.crypto_msg_encrypted_incomplete,
    ),
    ENCRYPTED_NO_PROVIDER(
        colorAttr = R.attr.openpgp_red,
        statusIconRes = R.drawable.status_lock_error,
        titleTextRes = R.string.crypto_msg_title_encrypted_unknown,
        descriptionTextRes = R.string.crypto_msg_encrypted_no_provider,
    ),
    UNSUPPORTED_ENCRYPTED(
        colorAttr = R.attr.openpgp_red,
        statusIconRes = R.drawable.status_lock_error,
        titleTextRes = R.string.crypto_msg_title_encrypted_unknown,
        descriptionTextRes = R.string.crypto_msg_unsupported_encrypted,
    ),
    UNSUPPORTED_SIGNED(
        colorAttr = R.attr.openpgp_grey,
        statusIconRes = Icons.Outlined.NoEncryption,
        titleTextRes = R.string.crypto_msg_title_encrypted_unknown,
        descriptionTextRes = R.string.crypto_msg_unsupported_signed,
    ),
    ;

    fun hasAssociatedKey(): Boolean {
        return when (this) {
            ENCRYPTED_SIGN_VERIFIED,
            ENCRYPTED_SIGN_UNVERIFIED,
            ENCRYPTED_SIGN_MISMATCH,
            ENCRYPTED_SIGN_EXPIRED,
            ENCRYPTED_SIGN_REVOKED,
            ENCRYPTED_SIGN_INSECURE,
            UNENCRYPTED_SIGN_VERIFIED,
            UNENCRYPTED_SIGN_UNVERIFIED,
            UNENCRYPTED_SIGN_MISMATCH,
            UNENCRYPTED_SIGN_EXPIRED,
            UNENCRYPTED_SIGN_REVOKED,
            UNENCRYPTED_SIGN_INSECURE,
            -> true

            else -> false
        }
    }

    val isUnencryptedSigned: Boolean
        get() = when (this) {
            UNENCRYPTED_SIGN_ERROR,
            UNENCRYPTED_SIGN_UNKNOWN,
            UNENCRYPTED_SIGN_VERIFIED,
            UNENCRYPTED_SIGN_UNVERIFIED,
            UNENCRYPTED_SIGN_MISMATCH,
            UNENCRYPTED_SIGN_EXPIRED,
            UNENCRYPTED_SIGN_REVOKED,
            UNENCRYPTED_SIGN_INSECURE,
            -> true

            else -> false
        }

    val isUnknownKey: Boolean
        get() = when (this) {
            ENCRYPTED_SIGN_UNKNOWN, UNENCRYPTED_SIGN_UNKNOWN -> true
            else -> false
        }

    companion object {
        @JvmStatic
        fun fromResultAnnotation(
            cryptoResult: CryptoResultAnnotation?,
        ): MessageCryptoDisplayStatus {
            return when (cryptoResult?.errorType) {
                null -> DISABLED
                CryptoError.OPENPGP_OK -> getDisplayStatusForPgpResult(cryptoResult)
                CryptoError.OPENPGP_ENCRYPTED_BUT_INCOMPLETE -> INCOMPLETE_ENCRYPTED
                CryptoError.OPENPGP_SIGNED_BUT_INCOMPLETE -> INCOMPLETE_SIGNED
                CryptoError.ENCRYPTED_BUT_UNSUPPORTED -> UNSUPPORTED_ENCRYPTED
                CryptoError.SIGNED_BUT_UNSUPPORTED -> UNSUPPORTED_SIGNED
                CryptoError.OPENPGP_UI_CANCELED -> CANCELLED
                CryptoError.OPENPGP_SIGNED_API_ERROR -> UNENCRYPTED_SIGN_ERROR
                CryptoError.OPENPGP_ENCRYPTED_API_ERROR -> ENCRYPTED_ERROR
                CryptoError.OPENPGP_ENCRYPTED_NO_PROVIDER -> ENCRYPTED_NO_PROVIDER
                else -> error("Unhandled case!")
            }
        }

        private fun getDisplayStatusForPgpResult(
            cryptoResult: CryptoResultAnnotation,
        ): MessageCryptoDisplayStatus {
            var signatureResult = cryptoResult.openPgpSignatureResult
            val decryptionResult = cryptoResult.openPgpDecryptionResult
            if (decryptionResult == null || signatureResult == null) {
                throw AssertionError("Both OpenPGP results must be non-null at this point!")
            }

            if (signatureResult.result == RESULT_NO_SIGNATURE && cryptoResult.hasEncapsulatedResult()) {
                val encapsulatedResult = cryptoResult.encapsulatedResult
                if (encapsulatedResult.isOpenPgpResult) {
                    signatureResult = encapsulatedResult.openPgpSignatureResult
                        ?: throw AssertionError("OpenPGP must contain signature result at this point!")
                }
            }

            return getStatusForPgpResult(decryptionResult.getResult(), signatureResult)
        }

        private fun getStatusForPgpResult(
            pgpResult: Int,
            signatureResult: OpenPgpSignatureResult,
        ): MessageCryptoDisplayStatus {
            return when (pgpResult) {
                RESULT_NOT_ENCRYPTED -> getStatusForPgpUnencryptedResult(signatureResult)
                RESULT_ENCRYPTED -> getStatusForPgpEncryptedResult(signatureResult)
                RESULT_INSECURE -> ENCRYPTED_INSECURE
                else -> throw AssertionError("all cases must be handled, this is a bug!")
            }
        }

        private fun getStatusForPgpEncryptedResult(
            signatureResult: OpenPgpSignatureResult,
        ): MessageCryptoDisplayStatus {
            return when (signatureResult.result) {
                RESULT_NO_SIGNATURE -> ENCRYPTED_UNSIGNED
                RESULT_VALID_KEY_CONFIRMED, RESULT_VALID_KEY_UNCONFIRMED ->
                    getStatusForPgpEncryptedSenderStatusResult(signatureResult.senderStatusResult)

                RESULT_KEY_MISSING -> ENCRYPTED_SIGN_UNKNOWN
                RESULT_INVALID_SIGNATURE -> ENCRYPTED_SIGN_ERROR
                RESULT_INVALID_KEY_EXPIRED -> ENCRYPTED_SIGN_EXPIRED
                RESULT_INVALID_KEY_REVOKED -> ENCRYPTED_SIGN_REVOKED
                RESULT_INVALID_KEY_INSECURE -> ENCRYPTED_SIGN_INSECURE
                else -> error("unhandled encrypted result case!")
            }
        }

        private fun getStatusForPgpEncryptedSenderStatusResult(
            senderStatusResult: SenderStatusResult,
        ): MessageCryptoDisplayStatus {
            return when (senderStatusResult) {
                USER_ID_CONFIRMED -> ENCRYPTED_SIGN_VERIFIED
                USER_ID_UNCONFIRMED -> ENCRYPTED_SIGN_UNVERIFIED
                USER_ID_MISSING -> ENCRYPTED_SIGN_MISMATCH
                UNKNOWN -> ENCRYPTED_SIGN_UNVERIFIED
                else -> error("unhandled encrypted result case!")
            }
        }

        private fun getStatusForPgpUnencryptedResult(
            signatureResult: OpenPgpSignatureResult,
        ): MessageCryptoDisplayStatus {
            return when (signatureResult.result) {
                RESULT_NO_SIGNATURE -> DISABLED
                RESULT_VALID_KEY_CONFIRMED, RESULT_VALID_KEY_UNCONFIRMED ->
                    getStatusForPgpUnencryptedSenderStatusResult(signatureResult.senderStatusResult)

                RESULT_KEY_MISSING -> UNENCRYPTED_SIGN_UNKNOWN
                RESULT_INVALID_SIGNATURE -> UNENCRYPTED_SIGN_ERROR
                RESULT_INVALID_KEY_EXPIRED -> UNENCRYPTED_SIGN_EXPIRED
                RESULT_INVALID_KEY_REVOKED -> UNENCRYPTED_SIGN_REVOKED
                RESULT_INVALID_KEY_INSECURE -> UNENCRYPTED_SIGN_INSECURE
                else -> error("unhandled encrypted result case!")
            }
        }

        private fun getStatusForPgpUnencryptedSenderStatusResult(
            senderStatusResult: SenderStatusResult,
        ): MessageCryptoDisplayStatus {
            return when (senderStatusResult) {
                USER_ID_CONFIRMED -> UNENCRYPTED_SIGN_VERIFIED
                USER_ID_UNCONFIRMED -> UNENCRYPTED_SIGN_UNVERIFIED
                USER_ID_MISSING -> UNENCRYPTED_SIGN_MISMATCH
                UNKNOWN -> UNENCRYPTED_SIGN_UNVERIFIED
                else -> error("unhandled encrypted result case!")
            }
        }
    }
}
