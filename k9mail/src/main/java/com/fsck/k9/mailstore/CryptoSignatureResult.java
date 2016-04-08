package com.fsck.k9.mailstore;

import com.fsck.k9.ui.crypto.CryptoMethod;

import org.openintents.openpgp.OpenPgpSignatureResult;
import org.openintents.smime.SmimeSignatureResult;

/**
 * Created by philip on 07/04/2016.
 */
public class CryptoSignatureResult {
    private final CryptoMethod method;
    private SmimeSignatureResult smimeSignatureResult;
    private OpenPgpSignatureResult openPgpSignatureResult;

    // content not signed
    public static final int RESULT_NO_SIGNATURE = -1;
    // invalid signature!
    public static final int RESULT_INVALID_SIGNATURE = 0;
    // successfully verified signature, with confirmed key
    public static final int RESULT_VALID_CONFIRMED = 1;
    // no key was found for this signature verification
    public static final int RESULT_KEY_MISSING = 2;
    // successfully verified signature, but with unconfirmed key
    public static final int RESULT_VALID_UNCONFIRMED = 3;
    // key has been revoked -> invalid signature!
    public static final int RESULT_INVALID_KEY_REVOKED = 4;
    // key is expired -> invalid signature!
    public static final int RESULT_INVALID_KEY_EXPIRED = 5;
    // insecure cryptographic algorithms/protocol -> invalid signature!
    public static final int RESULT_INVALID_INSECURE = 6;

    public CryptoSignatureResult(SmimeSignatureResult signatureResult) {
        method = CryptoMethod.SMIME;
        smimeSignatureResult = signatureResult;
    }

    public CryptoSignatureResult(OpenPgpSignatureResult signatureResult) {
        method = CryptoMethod.SMIME;
        openPgpSignatureResult = signatureResult;
    }

    public int getResult() {
        switch(method) {
            case OPENPGP:
                return openPgpSignatureResult.getResult();
            case SMIME:
                return smimeSignatureResult.getResult();
        }
        throw new AssertionError("Unknown method");
    }

    public String getPrimaryUserId() {
        switch(method) {
            case OPENPGP:
                return openPgpSignatureResult.getPrimaryUserId();
            case SMIME:
                return smimeSignatureResult.getPrimaryUserId();
        }
        throw new AssertionError("Unknown method");
    }
}
