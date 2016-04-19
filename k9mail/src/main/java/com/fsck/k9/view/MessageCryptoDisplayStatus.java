package com.fsck.k9.view;


import com.fsck.k9.mailstore.OpenPgpResultAnnotation;
import com.fsck.k9.mailstore.OpenPgpResultAnnotation.CryptoError;
import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpSignatureResult;


public enum MessageCryptoDisplayStatus {
    DISABLED,

    UNENCRYPTED_SIGN_UNKNOWN,
    UNENCRYPTED_SIGN_VERIFIED,
    UNENCRYPTED_SIGN_UNVERIFIED,
    UNENCRYPTED_SIGN_ERROR,
    UNENCRYPTED_SIGN_MISMATCH,
    UNENCRYPTED_SIGN_EXPIRED,
    UNENCRYPTED_SIGN_REVOKED,
    UNENCRYPTED_SIGN_INSECURE,

    ENCRYPTED_UNSIGNED,
    ENCRYPTED_SIGN_UNKNOWN,
    ENCRYPTED_SIGN_VERIFIED,
    ENCRYPTED_SIGN_UNVERIFIED,
    ENCRYPTED_SIGN_ERROR,
    ENCRYPTED_SIGN_MISMATCH,
    ENCRYPTED_SIGN_EXPIRED,
    ENCRYPTED_SIGN_REVOKED,
    ENCRYPTED_SIGN_INSECURE,
    ENCRYPTED_ERROR;

    public static MessageCryptoDisplayStatus fromResultAnnotation(OpenPgpResultAnnotation cryptoResult) {
        if (cryptoResult == null) {
            return DISABLED;
        }

        if (cryptoResult.getErrorType() != CryptoError.NONE) {
            return ENCRYPTED_ERROR;
        }

        OpenPgpSignatureResult signatureResult = cryptoResult.getSignatureResult();
        OpenPgpDecryptionResult decryptionResult = cryptoResult.getDecryptionResult();

        // TODO handle mismatched user id

        if (decryptionResult.getResult() == OpenPgpDecryptionResult.RESULT_NOT_ENCRYPTED) {
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

        if (decryptionResult.getResult() == OpenPgpDecryptionResult.RESULT_ENCRYPTED) {
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

        if (decryptionResult.getResult() == OpenPgpDecryptionResult.RESULT_INSECURE) {
            // TODO handle better?
            return ENCRYPTED_ERROR;
        }

        if (decryptionResult.getResult() == OpenPgpDecryptionResult.RESULT_ENCRYPTED) {
            return ENCRYPTED_ERROR;
        }

        throw new AssertionError("all cases must be handled, this is a bug!");
    }

}
