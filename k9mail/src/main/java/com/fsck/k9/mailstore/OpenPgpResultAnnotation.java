package com.fsck.k9.mailstore;


import android.app.PendingIntent;

import com.fsck.k9.mail.MessagingException;
import com.fsck.k9.mail.internet.MimeBodyPart;
import org.openintents.openpgp.OpenPgpError;
import org.openintents.openpgp.OpenPgpSignatureResult;


public class OpenPgpResultAnnotation extends MimeBodyPart {
    private boolean wasEncrypted;
    private OpenPgpSignatureResult signatureResult;
    private OpenPgpError error;
    private PendingIntent pendingIntent;

    public OpenPgpResultAnnotation(boolean wasEncrypted) throws MessagingException {
        this.wasEncrypted = wasEncrypted;
    }

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

    public boolean wasEncrypted() {
        return wasEncrypted;
    }
}
