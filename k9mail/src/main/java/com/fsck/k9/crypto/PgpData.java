package com.fsck.k9.crypto;

import java.io.Serializable;

import org.openintents.openpgp.OpenPgpSignatureResult;

public class PgpData implements Serializable {
    private static final long serialVersionUID = 6314045536470848410L;
    protected long mEncryptionKeyIds[] = null;
    protected long mSignatureKeyId = 0;
    protected String mSignatureUserId = null;
    protected String mDecryptedData = null;
    protected String mEncryptedData = null;
    
    // new API
    public void setEncryptionKeys(long keyIds[]) {
        mEncryptionKeyIds = keyIds;
    }

    public boolean hasEncryptionKeys() {
        return (mEncryptionKeyIds != null) && (mEncryptionKeyIds.length > 0);
    }

    public String getEncryptedData() {
        return mEncryptedData;
    }

    public void setEncryptedData(String data) {
        mEncryptedData = data;
    }

    public String getDecryptedData() {
        return mDecryptedData;
    }
}
