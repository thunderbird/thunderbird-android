package com.fsck.k9.mailstore;

import android.app.PendingIntent;

import com.fsck.k9.mail.Part;
import com.fsck.k9.ui.crypto.CryptoDecryptionResult;


public final class CryptoResultAnnotation {
    private CryptoSignatureResult signatureResult;
    private CryptoDecryptionResult decryptionResult;
    private PendingIntent pendingIntent;
    private Part outputData;
    private CryptoError error;
    private boolean encrypted;
    private boolean secure;

    public Part getOutputData() {
        return outputData;
    }
    public boolean hasOutputData() {
        return outputData != null;
    }
    public CryptoError getError() {
        return error;
    }

    public void setErrorType(CryptoErrorType errorType) {
        this.error.setErrorType(errorType);
    }

    public boolean isEncrypted() {
        return encrypted;
    }
    public boolean isSecure() {
        return secure;
    }

    public CryptoSignatureResult getSignatureResult() {
        return signatureResult;
    }

    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    public void setOutputData(Part outputData) {
        this.outputData = outputData;
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    public void setError(CryptoError error) {
        this.error = error;
    }

    public void setSignatureResult(CryptoSignatureResult signatureResult) {
        this.signatureResult = signatureResult;
    }

    public void setDecryptionResult(CryptoDecryptionResult decryptionResult) {
        this.decryptionResult = decryptionResult;
    }

    public CryptoErrorType getErrorType() {
        return error.getErrorType();
    }
}
