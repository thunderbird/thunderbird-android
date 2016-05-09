package com.fsck.k9.view;


import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

import com.fsck.k9.R;
import com.fsck.k9.mailstore.CryptoResultAnnotation;
import com.fsck.k9.mailstore.CryptoResultAnnotation.CryptoError;
import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpSignatureResult;


public enum MessageCryptoDisplayStatus {
    LOADING (
            R.color.openpgp_grey,
            R.drawable.status_lock
    ),

    DISABLED (
            R.color.openpgp_grey,
            R.drawable.status_lock_disabled,
            R.string.crypto_msg_disabled
    ),

    UNENCRYPTED_SIGN_UNKNOWN (
            R.color.openpgp_grey,
            R.drawable.status_signature_unverified_cutout, R.drawable.status_dots,
            R.string.crypto_msg_signed_unencrypted, null
    ),

    UNENCRYPTED_SIGN_VERIFIED (
            R.color.openpgp_blue,
            R.drawable.status_signature_verified_cutout, R.drawable.status_none_dots_3,
            R.string.crypto_msg_signed_unencrypted, R.string.crypto_msg_sign_verified
    ),
    UNENCRYPTED_SIGN_UNVERIFIED (
            R.color.openpgp_orange, 
            R.drawable.status_signature_verified_cutout, R.drawable.status_none_dots_2,
            R.string.crypto_msg_signed_unencrypted, R.string.crypto_msg_sign_unverified
    ),
    UNENCRYPTED_SIGN_ERROR (
            R.color.openpgp_red, 
            R.drawable.status_signature_verified_cutout, R.drawable.status_none_dots_1,
            R.string.crypto_msg_signed_unencrypted, R.string.crypto_msg_sign_error
    ),
    UNENCRYPTED_SIGN_MISMATCH (
            R.color.openpgp_red, 
            R.drawable.status_signature_verified_cutout, R.drawable.status_none_dots_1,
            R.string.crypto_msg_signed_unencrypted, R.string.crypto_msg_sign_mismatch
    ),
    UNENCRYPTED_SIGN_EXPIRED (
            R.color.openpgp_red, 
            R.drawable.status_signature_verified_cutout, R.drawable.status_none_dots_1,
            R.string.crypto_msg_signed_unencrypted, R.string.crypto_msg_sign_expired
    ),
    UNENCRYPTED_SIGN_REVOKED (
            R.color.openpgp_red, 
            R.drawable.status_signature_verified_cutout, R.drawable.status_none_dots_1,
            R.string.crypto_msg_signed_unencrypted, R.string.crypto_msg_sign_revoked
    ),
    UNENCRYPTED_SIGN_INSECURE (
            R.color.openpgp_red, 
            R.drawable.status_signature_verified_cutout, R.drawable.status_none_dots_1,
            R.string.crypto_msg_signed_unencrypted, R.string.crypto_msg_sign_insecure
    ),

    ENCRYPTED_SIGN_UNKNOWN (
            R.color.openpgp_grey, 
            R.drawable.status_lock_opportunistic, R.drawable.status_dots,
            R.string.crypto_msg_signed_encrypted, null
    ),

    ENCRYPTED_SIGN_VERIFIED (
            R.color.openpgp_green, 
            R.drawable.status_lock, R.drawable.status_none_dots_3,
            R.string.crypto_msg_signed_encrypted, R.string.crypto_msg_sign_verified
    ),
    ENCRYPTED_SIGN_UNVERIFIED (
            R.color.openpgp_orange, 
            R.drawable.status_lock, R.drawable.status_none_dots_2,
            R.string.crypto_msg_signed_encrypted, R.string.crypto_msg_sign_unverified
    ),
    ENCRYPTED_SIGN_ERROR (
            R.color.openpgp_red, 
            R.drawable.status_lock, R.drawable.status_none_dots_1,
            R.string.crypto_msg_signed_encrypted, R.string.crypto_msg_sign_error
    ),
    ENCRYPTED_SIGN_MISMATCH (
            R.color.openpgp_red, 
            R.drawable.status_lock, R.drawable.status_none_dots_1,
            R.string.crypto_msg_signed_encrypted, R.string.crypto_msg_sign_mismatch
    ),
    ENCRYPTED_SIGN_EXPIRED (
            R.color.openpgp_red, 
            R.drawable.status_lock, R.drawable.status_none_dots_1,
            R.string.crypto_msg_signed_encrypted, R.string.crypto_msg_sign_expired
    ),
    ENCRYPTED_SIGN_REVOKED (
            R.color.openpgp_red, 
            R.drawable.status_lock, R.drawable.status_none_dots_1,
            R.string.crypto_msg_signed_encrypted, R.string.crypto_msg_sign_revoked
    ),
    ENCRYPTED_SIGN_INSECURE (
            R.color.openpgp_red,
            R.drawable.status_lock, R.drawable.status_none_dots_1,
            R.string.crypto_msg_signed_encrypted, R.string.crypto_msg_sign_insecure
    ),
    ENCRYPTED_UNSIGNED (
            R.color.openpgp_red, 
            R.drawable.status_lock, R.drawable.status_none_dots_1,
            R.string.crypto_msg_encrypted_unsigned, R.string.crypto_msg_unsigned_encrypted
    ),

    ENCRYPTED_ERROR (
            R.color.openpgp_red, 
            R.drawable.status_lock_error,
            R.string.crypto_msg_encrypted_error
    ),

    SIGNED_UNSUPPORTED (
            R.color.openpgp_red,
            R.drawable.status_signature_verified_cutout,
            R.string.crypto_msg_signed_unsupported
    ),
    ;

    @ColorRes public final int color;
    @DrawableRes public final int iconResFirst;
    @DrawableRes public final Integer iconResSecond;

    @StringRes public final Integer textResFirst;
    @StringRes public final Integer textResSecond;

    MessageCryptoDisplayStatus(@ColorRes int color, @DrawableRes int iconResFirst, @DrawableRes Integer iconResSecond,
            @StringRes int textResFirst, @StringRes Integer textResSecond) {
        this.color = color;
        this.iconResFirst = iconResFirst;
        this.iconResSecond = iconResSecond;

        this.textResFirst = textResFirst;
        this.textResSecond = textResSecond;
    }

    MessageCryptoDisplayStatus(@ColorRes int color, @DrawableRes int iconResFirst, @StringRes int textResFirst) {
        this.color = color;
        this.iconResFirst = iconResFirst;
        this.iconResSecond = null;

        this.textResFirst = textResFirst;
        this.textResSecond = null;
    }

    MessageCryptoDisplayStatus(@ColorRes int color, @DrawableRes int iconResFirst) {
        this.color = color;
        this.iconResFirst = iconResFirst;
        this.iconResSecond = null;

        this.textResFirst = null;
        this.textResSecond = null;
    }

    @NonNull
    public static MessageCryptoDisplayStatus fromResultAnnotation(CryptoResultAnnotation cryptoResult) {
        if (cryptoResult == null) {
            return DISABLED;
        }

        if (cryptoResult.getErrorType() != CryptoError.NONE) {
            return ENCRYPTED_ERROR;
        }

        if (cryptoResult.isOpenPgpResult()) {
            return getDisplayStatusForPgpResult(cryptoResult);
        }

        throw new AssertionError("all cases must be handled, this is a bug!");
    }

    @NonNull
    private static MessageCryptoDisplayStatus getDisplayStatusForPgpResult(CryptoResultAnnotation cryptoResult) {
        OpenPgpSignatureResult signatureResult = cryptoResult.getOpenPgpSignatureResult();
        OpenPgpDecryptionResult decryptionResult = cryptoResult.getOpenPgpDecryptionResult();
        if (decryptionResult == null || signatureResult == null) {
            throw new AssertionError("Both OpenPGP results must be non-null at this point!");
        }

        if (signatureResult.getResult() == OpenPgpSignatureResult.RESULT_NO_SIGNATURE &&
                cryptoResult.hasEncapsulatedResult()) {
            CryptoResultAnnotation encapsulatedResult = cryptoResult.getEncapsulatedResult();
            if (encapsulatedResult.isOpenPgpResult()) {
                signatureResult = encapsulatedResult.getOpenPgpSignatureResult();
            }
        }

        // TODO handle mismatched user id

        switch (decryptionResult.getResult()) {
            case OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED:
                return getStatusForPgpUnencryptedResult(signatureResult);

            case OpenPgpDecryptionResult.RESULT_ENCRYPTED:
                return getStatusForPgpEncryptedResult(signatureResult);

            case OpenPgpDecryptionResult.RESULT_INSECURE:
                // TODO handle better?
                return ENCRYPTED_ERROR;
        }

        throw new AssertionError("all cases must be handled, this is a bug!");
    }

    @NonNull
    private static MessageCryptoDisplayStatus getStatusForPgpEncryptedResult(OpenPgpSignatureResult signatureResult) {
        switch (signatureResult.getResult()) {
            case OpenPgpSignatureResult.RESULT_NO_SIGNATURE:
                return ENCRYPTED_UNSIGNED;

            case OpenPgpSignatureResult.RESULT_VALID_CONFIRMED:
                return ENCRYPTED_SIGN_VERIFIED;

            case OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED:
                return ENCRYPTED_SIGN_UNVERIFIED;

            case OpenPgpSignatureResult.RESULT_KEY_MISSING:
                return ENCRYPTED_SIGN_UNKNOWN;

            case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE:
                return ENCRYPTED_SIGN_ERROR;

            case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
                return ENCRYPTED_SIGN_EXPIRED;

            case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
                return ENCRYPTED_SIGN_REVOKED;

            case OpenPgpSignatureResult.RESULT_INVALID_INSECURE:
                return ENCRYPTED_SIGN_INSECURE;

            default:
                throw new AssertionError("all cases must be handled, this is a bug!");
        }
    }

    @NonNull
    private static MessageCryptoDisplayStatus getStatusForPgpUnencryptedResult(OpenPgpSignatureResult signatureResult) {
        switch (signatureResult.getResult()) {
            case OpenPgpSignatureResult.RESULT_NO_SIGNATURE:
                return DISABLED;

            case OpenPgpSignatureResult.RESULT_VALID_CONFIRMED:
                return UNENCRYPTED_SIGN_VERIFIED;

            case OpenPgpSignatureResult.RESULT_VALID_UNCONFIRMED:
                return UNENCRYPTED_SIGN_UNVERIFIED;

            case OpenPgpSignatureResult.RESULT_KEY_MISSING:
                return UNENCRYPTED_SIGN_UNKNOWN;

            case OpenPgpSignatureResult.RESULT_INVALID_SIGNATURE:
                return UNENCRYPTED_SIGN_ERROR;

            case OpenPgpSignatureResult.RESULT_INVALID_KEY_EXPIRED:
                return UNENCRYPTED_SIGN_EXPIRED;

            case OpenPgpSignatureResult.RESULT_INVALID_KEY_REVOKED:
                return UNENCRYPTED_SIGN_REVOKED;

            case OpenPgpSignatureResult.RESULT_INVALID_INSECURE:
                return UNENCRYPTED_SIGN_INSECURE;

            default:
                throw new AssertionError("all cases must be handled, this is a bug!");
        }
    }

}
