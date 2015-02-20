package com.fsck.k9.mailstore;


import android.app.PendingIntent;

import com.fsck.k9.mail.internet.MimeBodyPart;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;


public final class OpenPgpResultAnnotation {
    private boolean wasEncrypted;
    private OpenPgpSignatureResult signatureResult;
    private OpenPgpError error;
    private CryptoError errorType = CryptoError.NONE;
    private PendingIntent pendingIntent;
    private MimeBodyPart outputData;

    public OpenPgpSignatureResult getSignatureResult() {
        return signatureResult;
    }

    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    public void setSignatureResult(OpenPgpSignatureResult signatureResult) {
        this.signatureResult = signatureResult;
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    public OpenPgpError getError() {
        return error;
    }

    public void setError(OpenPgpError error) {
        this.error = error;
        setErrorType(CryptoError.CRYPTO_API_RETURNED_ERROR);
    }

    public CryptoError getErrorType() {
        return errorType;
    }

    public void setErrorType(CryptoError errorType) {
        this.errorType = errorType;
    }

    public boolean hasOutputData() {
        return outputData != null;
    }

    public void setOutputData(MimeBodyPart outputData) {
        this.outputData = outputData;
    }

    public MimeBodyPart getOutputData() {
        return outputData;
    }

    public boolean wasEncrypted() {
        return wasEncrypted;
    }

    public void setWasEncrypted(boolean wasEncrypted) {
        this.wasEncrypted = wasEncrypted;
    }


    public static enum CryptoError {
        NONE,
        CRYPTO_API_RETURNED_ERROR,
        SIGNED_BUT_INCOMPLETE,
        ENCRYPTED_BUT_INCOMPLETE
    }
}
