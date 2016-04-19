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
    }


    public static CryptoResultAnnotation createOpenPgpResultAnnotation(OpenPgpDecryptionResult decryptionResult,
            OpenPgpSignatureResult signatureResult, PendingIntent pendingIntent, MimeBodyPart replacementPart) {
        return new CryptoResultAnnotation(CryptoError.NONE, replacementPart,
                decryptionResult, signatureResult, pendingIntent, null);
    }

    public static CryptoResultAnnotation createErrorAnnotation(CryptoError error, MimeBodyPart replacementData) {
        if (error == CryptoError.NONE) {
            throw new AssertionError("CryptoError must be actual error state!");
        }
        return new CryptoResultAnnotation(error, replacementData, null, null, null, null);
    }

    public static CryptoResultAnnotation createOpenPgpErrorAnnotation(OpenPgpError error) {
        return new CryptoResultAnnotation(CryptoError.OPENPGP_API_RETURNED_ERROR, null, null, null, null, error);
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


    public enum CryptoError {
        NONE,
        OPENPGP_API_RETURNED_ERROR,
        SIGNED_BUT_INCOMPLETE,
        ENCRYPTED_BUT_INCOMPLETE,
        SIGNED_BUT_UNSUPPORTED,
        ENCRYPTED_BUT_UNSUPPORTED,
    }
}
