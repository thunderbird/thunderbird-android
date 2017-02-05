package com.fsck.k9.mailstore;


import android.app.PendingIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.fsck.k9.mail.internet.MimeBodyPart;

import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;


public final class CryptoResultAnnotation {
    @NonNull private final CryptoError errorType;
    private final MimeBodyPart replacementData;

    private final OpenPgpDecryptionResult openPgpDecryptionResult;
    private final OpenPgpSignatureResult openPgpSignatureResult;
    private final OpenPgpError openPgpError;
    private final PendingIntent openPgpPendingIntent;

    private final CryptoResultAnnotation encapsulatedResult;

    private CryptoResultAnnotation(@NonNull CryptoError errorType, MimeBodyPart replacementData,
            OpenPgpDecryptionResult openPgpDecryptionResult,
            OpenPgpSignatureResult openPgpSignatureResult,
            PendingIntent openPgpPendingIntent, OpenPgpError openPgpError) {
        this.errorType = errorType;
        this.replacementData = replacementData;

        this.openPgpDecryptionResult = openPgpDecryptionResult;
        this.openPgpSignatureResult = openPgpSignatureResult;
        this.openPgpPendingIntent = openPgpPendingIntent;
        this.openPgpError = openPgpError;

        this.encapsulatedResult = null;
    }

    private CryptoResultAnnotation(CryptoResultAnnotation annotation, CryptoResultAnnotation encapsulatedResult) {
        if (annotation.encapsulatedResult != null) {
            throw new AssertionError("cannot replace an encapsulated result, this is a bug!");
        }

        this.errorType = annotation.errorType;
        this.replacementData = annotation.replacementData;

        this.openPgpDecryptionResult = annotation.openPgpDecryptionResult;
        this.openPgpSignatureResult = annotation.openPgpSignatureResult;
        this.openPgpPendingIntent = annotation.openPgpPendingIntent;
        this.openPgpError = annotation.openPgpError;

        this.encapsulatedResult = encapsulatedResult;
    }


    public static CryptoResultAnnotation createOpenPgpResultAnnotation(OpenPgpDecryptionResult decryptionResult,
            OpenPgpSignatureResult signatureResult, PendingIntent pendingIntent, MimeBodyPart replacementPart) {
        return new CryptoResultAnnotation(CryptoError.OPENPGP_OK, replacementPart,
                decryptionResult, signatureResult, pendingIntent, null);
    }

    public static CryptoResultAnnotation createErrorAnnotation(CryptoError error, MimeBodyPart replacementData) {
        if (error == CryptoError.OPENPGP_OK) {
            throw new AssertionError("CryptoError must be actual error state!");
        }
        return new CryptoResultAnnotation(error, replacementData, null, null, null, null);
    }

    public static CryptoResultAnnotation createOpenPgpCanceledAnnotation() {
        return new CryptoResultAnnotation(CryptoError.OPENPGP_UI_CANCELED, null, null, null, null, null);
    }

    public static CryptoResultAnnotation createOpenPgpSignatureErrorAnnotation(
            OpenPgpError error, MimeBodyPart replacementData) {
        return new CryptoResultAnnotation(
                CryptoError.OPENPGP_SIGNED_API_ERROR, replacementData, null, null, null, error);
    }

    public static CryptoResultAnnotation createOpenPgpEncryptionErrorAnnotation(OpenPgpError error) {
        return new CryptoResultAnnotation(CryptoError.OPENPGP_ENCRYPTED_API_ERROR, null, null, null, null, error);
    }

    public boolean isOpenPgpResult() {
        return openPgpDecryptionResult != null && openPgpSignatureResult != null;
    }

    public boolean hasSignatureResult() {
        return openPgpSignatureResult != null &&
                openPgpSignatureResult.getResult() != OpenPgpSignatureResult.RESULT_NO_SIGNATURE;
    }

    @Nullable
    public OpenPgpDecryptionResult getOpenPgpDecryptionResult() {
        return openPgpDecryptionResult;
    }

    @Nullable
    public OpenPgpSignatureResult getOpenPgpSignatureResult() {
        return openPgpSignatureResult;
    }

    @Nullable
    public PendingIntent getOpenPgpSigningKeyIntentIfAny() {
        if (hasSignatureResult()) {
            return getOpenPgpPendingIntent();
        }
        if (encapsulatedResult != null && encapsulatedResult.hasSignatureResult()) {
            return encapsulatedResult.getOpenPgpPendingIntent();
        }
        return null;
    }

    @Nullable
    public PendingIntent getOpenPgpPendingIntent() {
        return openPgpPendingIntent;
    }

    @Nullable
    public OpenPgpError getOpenPgpError() {
        return openPgpError;
    }

    @NonNull
    public CryptoError getErrorType() {
        return errorType;
    }

    public boolean hasReplacementData() {
        return replacementData != null;
    }

    @Nullable
    public MimeBodyPart getReplacementData() {
        return replacementData;
    }

    @NonNull
    public CryptoResultAnnotation withEncapsulatedResult(CryptoResultAnnotation resultAnnotation) {
        return new CryptoResultAnnotation(this, resultAnnotation);
    }

    public boolean hasEncapsulatedResult() {
        return encapsulatedResult != null;
    }

    public CryptoResultAnnotation getEncapsulatedResult() {
        return encapsulatedResult;
    }


    public enum CryptoError {
        OPENPGP_OK,
        OPENPGP_UI_CANCELED,
        OPENPGP_SIGNED_API_ERROR,
        OPENPGP_ENCRYPTED_API_ERROR,
        OPENPGP_SIGNED_BUT_INCOMPLETE,
        OPENPGP_ENCRYPTED_BUT_INCOMPLETE,
        SIGNED_BUT_UNSUPPORTED,
        ENCRYPTED_BUT_UNSUPPORTED,
    }
}
