package com.fsck.k9.mailstore;


import android.app.PendingIntent;

import com.fsck.k9.mail.internet.MimeBodyPart;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;


public class OpenPgpResultAnnotation {
    private boolean wasEncrypted;
    private OpenPgpSignatureResult signatureResult;
    private OpenPgpError error;
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

}
