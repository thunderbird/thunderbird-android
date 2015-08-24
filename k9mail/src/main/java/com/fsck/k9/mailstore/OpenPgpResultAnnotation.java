package com.fsck.k9.mailstore;


import android.app.PendingIntent;

import com.fsck.k9.mail.internet.MimeBodyPart;

import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;


public final class OpenPgpResultAnnotation {
    private OpenPgpDecryptionResult decryptionResult;
    private OpenPgpSignatureResult signatureResult;
    private OpenPgpError error;
    private CryptoError errorType = CryptoError.NONE;
    private PendingIntent pendingIntent;
    private MimeBodyPart outputData;

    public OpenPgpDecryptionResult getDecryptionResult() {
        return decryptionResult;
    }

    public void setDecryptionResult(OpenPgpDecryptionResult decryptionResult) {
        this.decryptionResult = decryptionResult;
    }

    public OpenPgpSignatureResult getSignatureResult() {
        return signatureResult;
    }

    public void setSignatureResult(OpenPgpSignatureResult signatureResult) {
        this.signatureResult = signatureResult;
    }

    public PendingIntent getPendingIntent() {
        return pendingIntent;
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


    public enum CryptoError {
        NONE,
        CRYPTO_API_RETURNED_ERROR,
        SIGNED_BUT_INCOMPLETE,
        ENCRYPTED_BUT_INCOMPLETE
    }
}
