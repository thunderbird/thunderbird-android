package com.fsck.k9.ui.crypto;

import org.openintents.openpgp.OpenPgpDecryptionResult;
import org.openintents.smime.SmimeDecryptionResult;


public class CryptoDecryptionResult {
    private SmimeDecryptionResult smimeDecryptionResult;
    private OpenPgpDecryptionResult openPgpDecryptionResult;
    private CryptoMethod method;

    public CryptoDecryptionResult(SmimeDecryptionResult decryptionResult) {
        method = CryptoMethod.SMIME;
        smimeDecryptionResult = decryptionResult;
    }

    public CryptoDecryptionResult(OpenPgpDecryptionResult decryptionResult) {
        method = CryptoMethod.OPENPGP;
        openPgpDecryptionResult = decryptionResult;
    }
}
