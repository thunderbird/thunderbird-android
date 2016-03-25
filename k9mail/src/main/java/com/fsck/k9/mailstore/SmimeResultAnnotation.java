package com.fsck.k9.mailstore;


import android.app.PendingIntent;

import com.fsck.k9.mail.internet.MimeBodyPart;

import org.openintents.smime.SmimeDecryptionResult;
import org.openintents.smime.SmimeError;
import org.openintents.smime.SmimeSignatureResult;


public final class SmimeResultAnnotation extends CryptoResultAnnotation {
    private SmimeDecryptionResult decryptionResult;
    private SmimeSignatureResult signatureResult;
    private SmimeError error;
    private CryptoError errorType = CryptoError.NONE;
    private PendingIntent pendingIntent;
    private MimeBodyPart outputData;

    public SmimeDecryptionResult getDecryptionResult() {
        return decryptionResult;
    }

    public void setDecryptionResult(SmimeDecryptionResult decryptionResult) {
        this.decryptionResult = decryptionResult;
    }

    public SmimeSignatureResult getSignatureResult() {
        return signatureResult;
    }

    public void setSignatureResult(SmimeSignatureResult signatureResult) {
        this.signatureResult = signatureResult;
    }

    public PendingIntent getPendingIntent() {
        return pendingIntent;
    }

    public void setPendingIntent(PendingIntent pendingIntent) {
        this.pendingIntent = pendingIntent;
    }

    public SmimeError getError() {
        return error;
    }

    public void setError(SmimeError error) {
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
