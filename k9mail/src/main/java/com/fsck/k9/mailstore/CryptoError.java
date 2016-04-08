package com.fsck.k9.mailstore;

import com.fsck.k9.ui.crypto.CryptoMethod;

import org.openintents.openpgp.OpenPgpError;
import org.openintents.smime.SmimeError;

public class CryptoError {
    private final CryptoMethod method;
    private OpenPgpError openPgpError;
    private SmimeError smimeError;
    private CryptoErrorType errorType;

    public CryptoError(SmimeError error) {
        method = CryptoMethod.SMIME;
        smimeError = error;
    }

    public CryptoError(OpenPgpError error) {
        method = CryptoMethod.OPENPGP;
        openPgpError = error;
    }

    public void setErrorType(CryptoErrorType errorType) {
        this.errorType = errorType;
    }

    public CryptoErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        switch (method) {
            case OPENPGP: return openPgpError.getMessage();
            case SMIME: return smimeError.getMessage();
        }
        throw new AssertionError("Unhandled method");
    }
}
